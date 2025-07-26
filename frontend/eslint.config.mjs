import { dirname } from "path";
import { fileURLToPath } from "url";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const compat = new FlatCompat({
  baseDirectory: __dirname,
});

const eslintConfig = [
  ...compat.extends("next/core-web-vitals", "next/typescript"),
  {
    rules: {
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
    }
  }
];

export default eslintConfig;
