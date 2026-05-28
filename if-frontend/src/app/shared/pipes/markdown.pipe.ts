import { Pipe, PipeTransform } from '@angular/core';

/**
 * Convierte Markdown básico a HTML seguro.
 * Cubre los patrones que usa Claude: headers, bold, italic, listas, código, hr.
 * Usa [innerHTML] de Angular (sanitiza automáticamente script/eventos).
 */
@Pipe({ name: 'markdown', standalone: true })
export class MarkdownPipe implements PipeTransform {

  transform(raw: string | null | undefined): string {
    if (!raw) return '';

    const lines = raw.split('\n');
    const out: string[] = [];
    let inList = false;

    const esc = (s: string) =>
      s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    const inline = (s: string) =>
      s
        .replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.+?)\*/g, '<em>$1</em>')
        .replace(/`([^`]+)`/g, '<code>$1</code>');

    const closeList = () => { if (inList) { out.push('</ul>'); inList = false; } };

    for (const raw of lines) {
      const e = esc(raw);

      // Headers
      if (/^### /.test(e)) { closeList(); out.push(`<h4>${inline(e.slice(4))}</h4>`); continue; }
      if (/^## /.test(e))  { closeList(); out.push(`<h3>${inline(e.slice(3))}</h3>`); continue; }
      if (/^# /.test(e))   { closeList(); out.push(`<h2>${inline(e.slice(2))}</h2>`); continue; }

      // Numbered list (1. item)
      const numMatch = e.match(/^\d+\. (.+)/);
      if (numMatch) {
        if (!inList) { out.push('<ol>'); inList = true; }
        out.push(`<li>${inline(numMatch[1])}</li>`);
        // track ol vs ul with a flag — simplify: treat both as list
        continue;
      }

      // Bullet list
      const bulletMatch = e.match(/^\s*[-*•] (.+)/);
      if (bulletMatch) {
        if (!inList) { out.push('<ul>'); inList = true; }
        out.push(`<li>${inline(bulletMatch[1])}</li>`);
        continue;
      }

      closeList();

      // Horizontal rule
      if (/^---+$/.test(raw.trim())) { out.push('<hr>'); continue; }

      // Blank line
      if (raw.trim() === '') { out.push('<br>'); continue; }

      // Normal paragraph
      out.push(`<p>${inline(e)}</p>`);
    }

    closeList();
    return out.join('');
  }
}
