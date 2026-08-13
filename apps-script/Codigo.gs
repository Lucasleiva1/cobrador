const SALES_FOLDER_NAME = 'Caja Simple - Ventas';
const SYSTEM_FOLDER_NAME = 'Caja Simple - Sistema';
const DEFAULT_MAX_AUTHORIZED_DEVICES = 3;
const APP_TIME_ZONE = 'America/Argentina/Buenos_Aires';

function doGet() {
  return jsonResponse_({ ok: true, service: 'Caja Simple - Respaldo de ventas' });
}

function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    const data = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    if (!safeEquals_(String(data.token || ''), configuredToken_())) {
      return jsonResponse_({ ok: false, error: 'unauthorized' });
    }
    const deviceId = String(data.deviceId || '').trim();
    const sale = normalizeSale_(data.sale);
    if (!deviceId || !sale) {
      return jsonResponse_({ ok: false, error: 'invalid_payload' });
    }

    lock.waitLock(30000);
    if (!authorizeDevice_(deviceId)) {
      return jsonResponse_({ ok: false, error: 'device_not_authorized' });
    }
    saveSale_(sale);
    return jsonResponse_({ ok: true, saleId: sale.id });
  } catch (error) {
    console.error(error && error.stack ? error.stack : error);
    return jsonResponse_({ ok: false, error: 'server_error' });
  } finally {
    if (lock.hasLock()) lock.releaseLock();
  }
}

function configuredToken_() {
  const token = PropertiesService.getScriptProperties().getProperty('DEVICE_TOKEN');
  if (!token) throw new Error('Falta configurar DEVICE_TOKEN en las propiedades del proyecto.');
  return token;
}

function authorizeDevice_(deviceId) {
  // Los emuladores y compilaciones de prueba nunca ocupan uno de los lugares reales.
  if (deviceId.indexOf('debug-') === 0) return false;
  const properties = PropertiesService.getScriptProperties();
  const stored = properties.getProperty('AUTHORIZED_DEVICES');
  const devices = stored ? JSON.parse(stored) : [];
  if (devices.includes(deviceId)) return true;
  if (devices.length >= authorizedDeviceLimit_()) return false;
  devices.push(deviceId);
  properties.setProperty('AUTHORIZED_DEVICES', JSON.stringify(devices));
  return true;
}

function authorizedDeviceLimit_() {
  const configured = Number(PropertiesService.getScriptProperties().getProperty('MAX_AUTHORIZED_DEVICES'));
  return Number.isSafeInteger(configured) && configured > 0 ? configured : DEFAULT_MAX_AUTHORIZED_DEVICES;
}

function normalizeSale_(raw) {
  if (!raw || typeof raw !== 'object') return null;
  const id = String(raw.id || '').trim();
  const createdAt = Number(raw.createdAt);
  const totalAmount = integerAmount_(raw.totalAmount);
  const receivedAmount = integerAmount_(raw.receivedAmount);
  const changeAmount = integerAmount_(raw.changeAmount);
  if (!id || !Number.isFinite(createdAt) || createdAt <= 0 || totalAmount <= 0 || receivedAmount < totalAmount || changeAmount < 0) return null;

  const items = Array.isArray(raw.items) ? raw.items.map(function (item) {
    const unitPrice = integerAmount_(item.unitPrice);
    const quantity = integerAmount_(item.quantity);
    return {
      description: String(item.description || '').trim(),
      unitPrice: unitPrice,
      quantity: quantity,
      subtotal: unitPrice * quantity,
    };
  }).filter(function (item) { return item.unitPrice > 0 && item.quantity > 0; }) : [];
  if (!items.length || items.reduce(function (sum, item) { return sum + item.subtotal; }, 0) !== totalAmount) return null;
  return { id: id, createdAt: createdAt, totalAmount: totalAmount, receivedAmount: receivedAmount, changeAmount: changeAmount, items: items };
}

function integerAmount_(value) {
  const number = Number(value);
  return Number.isSafeInteger(number) ? number : -1;
}

function saveSale_(sale) {
  const day = Utilities.formatDate(new Date(sale.createdAt), APP_TIME_ZONE, 'yyyy-MM-dd');
  const systemDayFolder = childFolder_(childFolder_(DriveApp.getRootFolder(), SYSTEM_FOLDER_NAME), day);
  const ledgerName = 'ventas-' + day + '.json';
  const ledgerFile = firstFile_(systemDayFolder, ledgerName);
  const sales = ledgerFile ? JSON.parse(ledgerFile.getBlob().getDataAsString('UTF-8') || '[]') : [];
  if (!sales.some(function (saved) { return saved.id === sale.id; })) sales.push(sale);
  sales.sort(function (left, right) { return left.createdAt - right.createdAt || left.id.localeCompare(right.id); });
  const ledgerContent = JSON.stringify(sales);
  if (ledgerFile) ledgerFile.setContent(ledgerContent); else systemDayFolder.createFile(ledgerName, ledgerContent, MimeType.PLAIN_TEXT);

  const visibleDayFolder = childFolder_(childFolder_(DriveApp.getRootFolder(), SALES_FOLDER_NAME), day);
  const csvName = 'ventas-' + day + '.csv';
  const csvFile = firstFile_(visibleDayFolder, csvName);
  const csv = createReadableCsv_(sales);
  if (csvFile) csvFile.setContent(csv); else visibleDayFolder.createFile(csvName, csv, MimeType.CSV);
}

function createReadableCsv_(sales) {
  const rows = [['Fecha', 'Hora', 'Producto', 'Cantidad', 'Precio unitario', 'Subtotal', 'Total de la venta', 'Pagó con', 'Vuelto']];
  sales.forEach(function (sale) {
    const date = new Date(sale.createdAt);
    const day = Utilities.formatDate(date, APP_TIME_ZONE, 'dd/MM/yyyy');
    const time = Utilities.formatDate(date, APP_TIME_ZONE, 'HH:mm:ss');
    sale.items.forEach(function (item, index) {
      rows.push([
        day,
        time,
        item.description || 'Sin detalle',
        item.quantity,
        item.unitPrice,
        item.subtotal,
        index === 0 ? sale.totalAmount : '',
        index === 0 ? sale.receivedAmount : '',
        index === 0 ? sale.changeAmount : '',
      ]);
    });
  });
  return '\uFEFF' + rows.map(function (row) { return row.map(csvCell_).join(','); }).join('\r\n') + '\r\n';
}

function childFolder_(parent, name) {
  const folders = parent.getFoldersByName(name);
  return folders.hasNext() ? folders.next() : parent.createFolder(name);
}

function firstFile_(folder, name) {
  const files = folder.getFilesByName(name);
  return files.hasNext() ? files.next() : null;
}

function csvCell_(value) {
  const text = String(value == null ? '' : value);
  return /[",\r\n]/.test(text) ? '"' + text.replace(/"/g, '""') + '"' : text;
}

function safeEquals_(left, right) {
  if (left.length !== right.length) return false;
  let difference = 0;
  for (let index = 0; index < left.length; index += 1) difference |= left.charCodeAt(index) ^ right.charCodeAt(index);
  return difference === 0;
}

function jsonResponse_(body) {
  return ContentService.createTextOutput(JSON.stringify(body)).setMimeType(ContentService.MimeType.JSON);
}

// Usar manualmente sólo si se reemplazan los tres teléfonos autorizados.
function resetAuthorizedDevices() {
  PropertiesService.getScriptProperties().deleteProperty('AUTHORIZED_DEVICES');
}
