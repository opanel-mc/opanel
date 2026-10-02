# `ansi-to-html`

A TypeScript library for converting text containing ANSI escape sequences into HTML. Supports text styles, standard and bright colors, 256-color and RGB sequences, OSC 8 hyperlinks, and optional state persistence between conversions.

This was originally a port of the ansi to html converter from [bcat](https://github.com/rtomayko/bcat/blob/master/lib/bcat/ansi.rb) to JavaScript. It has since undergone quite a lot of modification.

## Source and attribution

The ANSI parsing and HTML conversion code in this directory is based on the
original [`ansi-to-html`](https://github.com/rburns/ansi-to-html) and has been
modified and maintained by OPanel developers for use in OPanel.

- Original author: [Rob Burns](https://github.com/rburns)
- Upstream project: [`rburns/ansi-to-html`](https://github.com/rburns/ansi-to-html)
- License: [MIT License](./LICENSE)
- Original copyright notice: Copyright (c) 2012 Rob Burns

This version has been converted to TypeScript with ES module exports while
retaining the original ANSI style handling and adding OSC 8 hyperlink support.

This OPanel-modified version remains subject to the following MIT License. The
original copyright and permission notices are retained.

## License

[MIT](./LICENSE)
