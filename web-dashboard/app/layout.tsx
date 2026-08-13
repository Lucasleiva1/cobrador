import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Caja Simple · Ventas",
  description: "Visor privado de archivos de ventas de Caja Simple",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="es"><body>{children}</body></html>;
}

