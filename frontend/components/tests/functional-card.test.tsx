import type { ExoticComponent, PropsWithChildren } from "react";
import { cleanup, render } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import userEvent from "@testing-library/user-event";
import { Circle } from "lucide-react";
import { FunctionalCard } from "../functional-card";
import { Dialog, DialogContent, DialogTrigger } from "../ui/dialog";

describe("test functional card", () => {
  afterEach(() => cleanup());

  it("should be correctly rendered", () => {
    const elem = render(
      <FunctionalCard
        icon={Circle}
        title="Test Card">
        <span>Hello World</span>
      </FunctionalCard>
    );

    expect(elem.getByText("Test Card")).toBeInTheDocument();
    expect(elem.getByTestId("functional-card-content")).toHaveTextContent("Hello World");
  });

  it("should render the link when moreLink is provided", () => {
    const link = "https://example.com";
    const elem = render(
      <FunctionalCard
        icon={Circle}
        title="Test Card"
        moreLink={link}>
        <span>Hello World</span>
      </FunctionalCard>
    );

    expect(elem.getByTestId("functional-card-more-link")).toHaveAttribute("href", link);
  });

  it("should render the dialog when moreDialog is provided", async () => {
    const user = userEvent.setup();
    const dialog = ({
      children,
      asChild
    }: PropsWithChildren & {
      asChild?: boolean
    }) => (
      <Dialog>
        <DialogTrigger asChild={asChild}>{children}</DialogTrigger>
        <DialogContent>
          <span>Hello Dialog</span>
        </DialogContent>
      </Dialog>
    );
    const elem = render(
      <FunctionalCard
        icon={Circle}
        title="Test Card"
        moreDialog={dialog as ExoticComponent}>
        <span>Hello World</span>
      </FunctionalCard>
    );

    await user.click(elem.getByTestId("functional-card-more-dialog-button"));
    expect(elem.getByText("Hello Dialog")).toBeInTheDocument();
  });
});
