import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator
} from "@/components/ui/breadcrumb";
import { Separator } from "@/components/ui/separator";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { cn } from "@/lib/utils";

export function SubPage({
  children,
  title,
  subTitle,
  icon,
  noScrollPage = false,
  className,
  ...props
}: Readonly<React.ComponentProps<"div"> & {
  children?: React.ReactNode
  title: string
  subTitle?: string
  icon?: React.ReactNode
  noScrollPage?: boolean
  className?: string
}>) {
  return (
    <div className={cn("px-16 max-md:px-12 max-sm:px-8 flex-1 flex flex-col gap-6 max-md:gap-2", noScrollPage && "max-h-screen")}>
      <div className="pt-10 pb-5 flex flex-col gap-8">
        <div className="flex items-center gap-4">
          <SidebarTrigger className="cursor-pointer"/>
          <Separator orientation="vertical"/>
          <Breadcrumb>
            <BreadcrumbList>
              <BreadcrumbItem>OPanel</BreadcrumbItem>
              <BreadcrumbSeparator />
              {subTitle && (
                <>
                  <BreadcrumbItem>{title}</BreadcrumbItem>
                  <BreadcrumbSeparator />
                </>
              )}
              <BreadcrumbItem>
                <BreadcrumbPage>{subTitle ?? title}</BreadcrumbPage>
              </BreadcrumbItem>
            </BreadcrumbList>
          </Breadcrumb>
        </div>
        <div className="flex items-center gap-5">
          {icon}
          <h1 className="text-3xl font-bold">{subTitle ?? title}</h1>
        </div>
      </div>
      <div className={cn(className, "pb-16")} {...props}>
        {children}
      </div>
    </div>
  );
}
