"use client";

import { useEffect } from "react";
import { cn } from "@/lib/utils";
import { Navbar } from "@/components/navbar";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbList,
  BreadcrumbSeparator
} from "@/components/ui/breadcrumb";

export function SubPage({
  children,
  title,
  subTitle,
  description,
  category,
  icon,
  showHeader = true,
  outerClassName,
  pageClassName,
  className,
  ...props
}: Readonly<React.ComponentProps<"div"> & {
  children?: React.ReactNode
  title: string
  subTitle?: string
  description?: string
  category?: string
  icon?: React.ReactNode
  showHeader?: boolean
  outerClassName?: string
  pageClassName?: string
  className?: string
}>) {
  useEffect(() => {
    document.title = `OPanel - ${subTitle ?? title}`;
  }, [title, subTitle]);

  return (
    <div className={cn("group max-h-[100dvh] bg-sidebar flex-1 flex flex-col", outerClassName)}>
      <div className="flex-1 min-h-0 flex flex-col overflow-y-auto">
        <Navbar
          className="sticky top-0 z-10 shrink-0 bg-background/70 px-8 backdrop-blur-lg max-sm:px-2"
          title={title}
          subTitle={subTitle}/>
        <div className={cn(
          "flex-1 p-8 max-sm:p-4 flex flex-col",
          description ? "gap-4" : "gap-8",
          showHeader ? pageClassName : className
        )}>
          {
            showHeader
            ? (
              <>
                <div className="space-y-4 max-sm:hidden">
                  {category && (
                    <Breadcrumb className="mb-3">
                      <BreadcrumbList>
                        <BreadcrumbItem>{category}</BreadcrumbItem>
                        {subTitle && (
                          <>
                            <BreadcrumbSeparator />
                            <BreadcrumbItem>{title}</BreadcrumbItem>
                          </>
                        )}
                      </BreadcrumbList>
                    </Breadcrumb>
                  )}
                  <div className="flex items-center gap-5">
                    {icon}
                    <h1 className="text-3xl font-bold">{subTitle ?? title}</h1>
                  </div>
                  {description && <span className="text-muted-foreground">{description}</span>}
                </div>
                <div className={className} {...props}>
                  {children}
                </div>
              </>
            )
            : children
          }
        </div>
      </div>
    </div>
  );
}
