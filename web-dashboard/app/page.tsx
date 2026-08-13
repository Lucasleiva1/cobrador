"use client";

import { ChangeEvent, useMemo, useState } from "react";

type CsvRow = {
  venta_id: string;
  fecha: string;
  hora: string;
  producto: string;
  cantidad: string;
  precio_unitario: string;
  subtotal: string;
  total: string;
  recibido: string;
  vuelto: string;
};

type Sale = {
  id: string;
  date: string;
  time: string;
  total: number;
  received: number;
  change: number;
  items: Array<{ product: string; quantity: number; price: number; subtotal: number }>;
};

const demoRows: CsvRow[] = [
  { venta_id: "ejemplo-1", fecha: new Date().toISOString().slice(0, 10), hora: "10:35:00", producto: "Sin detalle", cantidad: "1", precio_unitario: "50000", subtotal: "50000", total: "50000", recibido: "60000", vuelto: "10000" },
  { venta_id: "ejemplo-2", fecha: new Date().toISOString().slice(0, 10), hora: "12:10:00", producto: "Producto de ejemplo", cantidad: "2", precio_unitario: "7500", subtotal: "15000", total: "15000", recibido: "20000", vuelto: "5000" },
];

function splitCsvLine(line: string): string[] {
  const result: string[] = [];
  let field = "";
  let quoted = false;
  for (let index = 0; index < line.length; index += 1) {
    const char = line[index];
    if (char === '"' && quoted && line[index + 1] === '"') { field += '"'; index += 1; }
    else if (char === '"') quoted = !quoted;
    else if (char === "," && !quoted) { result.push(field); field = ""; }
    else field += char;
  }
  result.push(field);
  return result;
}

function parseCsv(text: string): CsvRow[] {
  const lines = text.replace(/^\uFEFF/, "").split(/\r?\n/).filter(Boolean);
  if (lines.length < 2) return [];
  const headers = splitCsvLine(lines[0]);
  return lines.slice(1).map((line) => {
    const values = splitCsvLine(line);
    return Object.fromEntries(headers.map((header, index) => [header, values[index] ?? ""])) as CsvRow;
  });
}

function groupSales(rows: CsvRow[]): Sale[] {
  const grouped = new Map<string, Sale>();
  rows.forEach((row) => {
    const sale = grouped.get(row.venta_id) ?? {
      id: row.venta_id, date: row.fecha, time: row.hora,
      total: Number(row.total), received: Number(row.recibido), change: Number(row.vuelto), items: [],
    };
    sale.items.push({ product: row.producto, quantity: Number(row.cantidad), price: Number(row.precio_unitario), subtotal: Number(row.subtotal) });
    grouped.set(row.venta_id, sale);
  });
  return [...grouped.values()].sort((a, b) => b.time.localeCompare(a.time));
}

const money = (value: number) => `$${new Intl.NumberFormat("es-AR", { maximumFractionDigits: 0 }).format(value)}`;

export default function Home() {
  const [rows, setRows] = useState<CsvRow[]>(demoRows);
  const [fileName, setFileName] = useState("Datos de ejemplo");
  const [error, setError] = useState("");
  const sales = useMemo(() => groupSales(rows), [rows]);
  const total = sales.reduce((sum, sale) => sum + sale.total, 0);

  async function openFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    const parsed = parseCsv(await file.text());
    if (!parsed.length || !parsed[0].venta_id) { setError("El archivo no tiene el formato de Caja Simple."); return; }
    setRows(parsed); setFileName(file.name); setError("");
  }

  return (
    <main>
      <header className="topbar">
        <div><span className="eyebrow">CAJA SIMPLE</span><h1>Ventas del día</h1></div>
        <div className="actions">
          <label className="button primary">Abrir archivo de Drive<input type="file" accept=".csv,text/csv" onChange={openFile} /></label>
          <button className="button secondary" onClick={() => window.print()}>Guardar PDF</button>
        </div>
      </header>
      <section className="fileline"><span className="dot" />{fileName}<small>El archivo se procesa solamente en este navegador.</small></section>
      {error && <p className="error">{error}</p>}
      <section className="summary">
        <article><span>Cantidad de ventas</span><strong>{sales.length}</strong></article>
        <article><span>Total vendido</span><strong>{money(total)}</strong></article>
        <article><span>Fecha</span><strong className="date">{sales[0]?.date ?? "—"}</strong></article>
      </section>
      <section className="sales">
        <div className="sectionTitle"><h2>Detalle</h2><span>{sales.length} operaciones</span></div>
        {sales.map((sale, index) => (
          <article className="sale" key={sale.id}>
            <div className="saleHead"><div><b>Venta {sales.length - index}</b><span>{sale.time.slice(0, 5)}</span></div><strong>{money(sale.total)}</strong></div>
            <div className="items">{sale.items.map((item, itemIndex) => (
              <div className="item" key={`${sale.id}-${itemIndex}`}><span>{item.product}</span><span>{item.quantity} × {money(item.price)}</span><b>{money(item.subtotal)}</b></div>
            ))}</div>
            <div className="payment"><span>Recibido <b>{money(sale.received)}</b></span><span>Vuelto <b>{money(sale.change)}</b></span></div>
          </article>
        ))}
      </section>
    </main>
  );
}

