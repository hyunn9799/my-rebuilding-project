import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium ring-offset-background transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90 shadow-[0_4px_20px_-4px_hsl(var(--primary)/0.3)] hover:shadow-[0_8px_30px_-8px_hsl(var(--primary)/0.4)] active:scale-[0.98]",
        destructive: "bg-destructive text-destructive-foreground hover:bg-destructive/90 shadow-[0_4px_20px_-4px_hsl(var(--destructive)/0.3)]",
        outline: "border-2 border-primary bg-transparent text-primary hover:bg-primary hover:text-primary-foreground",
        secondary: "bg-secondary text-secondary-foreground hover:bg-secondary/80 shadow-[0_2px_12px_-2px_hsl(220_20%_20%/0.06)]",
        ghost: "hover:bg-muted hover:text-foreground",
        link: "text-primary underline-offset-4 hover:underline",
        success: "bg-success text-success-foreground hover:bg-success/90 shadow-[0_4px_20px_-4px_hsl(var(--success)/0.3)]",
        warning: "bg-warning text-warning-foreground hover:bg-warning/90 shadow-[0_4px_20px_-4px_hsl(var(--warning)/0.3)]",
        accent: "bg-accent text-accent-foreground hover:bg-accent/90 shadow-[0_4px_20px_-4px_hsl(var(--accent)/0.3)] hover:shadow-[0_8px_30px_-8px_hsl(var(--accent)/0.4)] active:scale-[0.98]",
        hero: "bg-gradient-to-r from-[hsl(var(--primary))] to-[hsl(180_45%_45%)] text-primary-foreground shadow-[0_8px_30px_-8px_hsl(var(--primary)/0.4)] hover:shadow-[0_4px_20px_-4px_hsl(var(--primary)/0.3)] hover:opacity-95 active:scale-[0.98]",
      },
      size: {
        default: "h-10 px-5 py-2",
        sm: "h-9 rounded-md px-4",
        lg: "h-12 rounded-lg px-8 text-base",
        xl: "h-14 rounded-xl px-10 text-lg",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />;
  },
);
Button.displayName = "Button";

export { Button, buttonVariants };
