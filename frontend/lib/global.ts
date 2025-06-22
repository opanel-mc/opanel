export const version = "1.0.0";
export const apiUrl = (
  process.env.NODE_ENV === "development"
  ? `http://localhost:3000`
  : ""
);
