import type { TranslationKey } from "@/lang";
import { memo } from "react";
import { localize, localizeRich } from "@/lib/i18n";

// Memoize the component to avoid `args` props triggering re-renders
export const Text = memo(function Text({
  id,
  rich = true,
  args = [],
  className
}: {
  id: TranslationKey
  rich?: boolean
  args?: (string | React.ReactNode)[]
  className?: string
}) {
  const content = rich ? localizeRich(id, ...args) : localize(id);

  if(!rich) {
    return <span className={className}>{content}</span>;
  }

  return (
    <span
      className={className}
      dangerouslySetInnerHTML={{ __html: content }}
      suppressHydrationWarning/>
  );
}, (prev, next) => (
  prev.id === next.id
  && prev.rich === next.rich
  && prev.className === next.className
));
