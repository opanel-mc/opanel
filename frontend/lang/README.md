# i18n

The JSON files in this folder contain text strings for the frontend, with each file corresponding to a specific language.

## ID Naming Convention

An ID of a text should be named with following convention.

```
<page>.[...<component>].<part>
```

- `<page>`: The name of the page (can be omitted if the text is in an independent component)
- `[...<component>]`: The name of the component, which can be hierarchically nested from top to bottom
- `<part>`: The name of the component part, like `placeholder`, `description` or `tooltip`

- When naming an id of a browser tab title, the id should only be the page name.
- Error message id should use `error` as its component.
- All the names should be written in **kebab case**.
