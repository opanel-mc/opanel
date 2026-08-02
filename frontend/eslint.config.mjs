import { globalIgnores } from "eslint/config";
import next from "@next/eslint-plugin-next";
import typescriptEslint from "@typescript-eslint/eslint-plugin";
import typescriptParser from "@typescript-eslint/parser";
import importPlugin from "eslint-plugin-import";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import stylistic from "@stylistic/eslint-plugin";
import globals from "globals";

const eslintConfig = [
  globalIgnores([
    "scripts/**",
    "build/**",
    "dist/**",
    ".next/**",
    ".vinext/**",
    "components/ui/**",
    "wasm-lib/pkg/**",
    "wasm-lib/target/**",
  ]),
  {
    files: ["**/*.{js,mjs,ts,tsx}"],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
      },
      parser: typescriptParser,
      parserOptions: {
        ecmaFeatures: { jsx: true },
        ecmaVersion: "latest",
        sourceType: "module",
      },
    },
    settings: {
      react: { version: "detect" },
    },
    plugins: {
      "@next/next": next,
      "@typescript-eslint": typescriptEslint,
      react,
      "react-hooks": reactHooks,
      import: importPlugin,
      "@stylistic": stylistic,
    },
    rules: {
      ...typescriptEslint.configs.recommended.rules,
      ...react.configs.recommended.rules,
      ...react.configs["jsx-runtime"].rules,
      ...reactHooks.configs.recommended.rules,
      ...next.configs.recommended.rules,
      ...next.configs["core-web-vitals"].rules,
      "@typescript-eslint/no-explicit-any": "off",
      "@next/next/no-img-element": "off",
      "@typescript-eslint/no-unused-expressions": "off",
      "@typescript-eslint/no-unused-vars": ["error", {
        "args": "none",
        "caughtErrors": "none"
      }],
      "@typescript-eslint/consistent-type-imports": "warn",
      "import/order": ["warn", {
        groups: [
          "type",
          "builtin",
          "external",
        ],
        "newlines-between": "ignore"
      }],
      "import/first": "error",
      "import/no-duplicates": "error",
      "import/no-named-as-default": "off",
      "keyword-spacing": ["error", {
        "after": true,
        "overrides": {
          "if": { "after": false },
          "for": { "after": false },
          "while": { "after": false },
          "switch": { "after": false },
          "with": { "after": false },
        }
      }],
      "brace-style": ["error", "1tbs", { "allowSingleLine": true }],
      "no-unneeded-braces": "off",
      "arrow-body-style": ["error", "as-needed"],
      "react/jsx-closing-bracket-location": ["error", "after-props"],
      "react/jsx-closing-tag-location": ["error", "tag-aligned"],
      "react/display-name": "off",
      "@stylistic/implicit-arrow-linebreak": ["error", "beside"],
      "@stylistic/nonblock-statement-body-position": ["error", "beside"],
    }
  }
];

export default eslintConfig;
