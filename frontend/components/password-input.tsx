import { useState } from "react";
import { Eye, EyeClosed } from "lucide-react";
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput } from "./ui/input-group";

export function PasswordInput({
  ...props
}: React.ComponentProps<typeof InputGroupInput>) {
  const [visible, setVisible] = useState(false);

  return (
    <InputGroup>
      <InputGroupInput
        type={visible ? "text" : "password"}
        autoComplete="off"
        {...props}/>
      <InputGroupAddon align="inline-end">
        <InputGroupButton onClick={() => setVisible((current) => !current)}>
          {
            visible
            ? <Eye />
            : <EyeClosed />
          }
        </InputGroupButton>
      </InputGroupAddon>
    </InputGroup>
  );
}
