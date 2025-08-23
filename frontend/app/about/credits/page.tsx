"use client";

import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableRow
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import { minecraftAE } from "@/lib/fonts";

const contributors = {
  "Norcleeh": "https://github.com/NriotHrreion",
  "Deed": "https://github.com/henlowornd",
  "SeaMite": "https://github.com/SeaMite43981045"
};

function Contributor({ name }: {
  name: keyof typeof contributors
}) {
  return (
    <Link href={contributors[name]} target="_blank">
      {name}
    </Link>
  );
}

export default function Credits() {
  return (
    <Card className="w-3xl max-md:rounded-none">
      <CardHeader>
        <CardTitle>参与人员</CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <p>
          <span className={cn("text-theme font-semibold", minecraftAE.className)}>OPanel</span> 的诞生离不开下面这些人的参与和贡献！
        </p>
        <Table>
          <TableBody className="[&_td:nth-child(2)]:text-right [&_td:nth-child(2)]:space-x-2">
            <TableRow>
              <TableCell>核心开发</TableCell>
              <TableCell>
                <Contributor name="Norcleeh"/>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>开发</TableCell>
              <TableCell>
                <Contributor name="SeaMite"/>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>测试</TableCell>
              <TableCell>
                <Contributor name="Deed"/>
                <Contributor name="SeaMite"/>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Logo设计</TableCell>
              <TableCell>
                <Contributor name="Deed"/>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
      <CardFooter>
        <Button
          className="cursor-pointer"
          variant="link"
          onClick={() => window.location.href = "/about"}>
          <ChevronLeft />
          返回
        </Button>
      </CardFooter>
    </Card>
  );
}
