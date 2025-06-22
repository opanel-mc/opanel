import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator
} from "@/components/ui/breadcrumb";
import { Separator } from "@/components/ui/separator";
import { SidebarTrigger } from "@/components/ui/sidebar";

export function SubPage({
  children,
  title,
  icon
}: Readonly<{
  children?: React.ReactNode
  title: string
  icon?: React.ReactNode
}>) {
  return (
    <div className="flex flex-col gap-6">
      <div className="px-16 py-10 flex flex-col gap-8">
        <div className="flex items-center gap-4">
          <SidebarTrigger className="cursor-pointer"/>
          <Separator orientation="vertical"/>
          <Breadcrumb>
            <BreadcrumbList>
              <BreadcrumbItem>OPanel</BreadcrumbItem>
              <BreadcrumbSeparator />
              <BreadcrumbItem>
                <BreadcrumbPage>{title}</BreadcrumbPage>
              </BreadcrumbItem>
            </BreadcrumbList>
          </Breadcrumb>
        </div>
        <div className="flex items-center gap-5">
          {icon}
          <h1 className="text-3xl font-bold">{title}</h1>
        </div>
      </div>
      <div>
        {children}
      </div>
    </div>
  );
}
