import { NavItem } from "./nav-item/nav-item";

export const navItems: NavItem[] = [
  { navCap: "Home" },

  {
    displayName: "Dashboard",
    iconName: "tablerHome",
    bgcolor: "primary",
    route: "/apps/dashboard",
  },
  {
    displayName: "Files",
    iconName: "tablerFileCheck",
    bgcolor: "primary",
    route: "apps/Files",
  },
  {
    displayName: "Admin",
    iconName: "tablerShield", // or 'tablerShieldLock' / 'tablerUserCog'
    bgcolor: "primary",
    route: "apps/admin",
  },
  {
    displayName: "Consumer",
    iconName: "tablerUser",
    bgcolor: "primary",
    route: "apps/consumer",
  },

  { navCap: "Other" },

  {
    displayName: "Disabled",
    iconName: "tablerBan",
    bgcolor: "primary",
    disabled: true,
  },
  {
    displayName: "Chip",
    iconName: "tablerMoodSmile",
    bgcolor: "warning",
    route: "/",
    chip: true,
    chipClass: "bg-primary text-white",
    chipContent: "9",
  },

  {
    displayName: "Outlined",
    iconName: "tablerMoodSmile",
    bgcolor: "success",
    route: "/",
    chip: true,
    chipClass: "b-1 border-primary text-primary",
    chipContent: "outlined",
  },
  {
    displayName: "External Link",
    iconName: "tablerStar",
    bgcolor: "error",
    route: "https://www.google.com/",
    external: true,
  },
  {
    displayName: "Invoice",
    iconName: "tablerFileInvoice",
    bgcolor: "primary",
    route: "/apps/invoice",
  },
  {
    displayName: "Document",
    iconName: "tablerFiles",
    bgcolor: "primary",
    route: "apps/document",
  },
  {
    displayName: "Document Upload",
    iconName: "tablerCloudUpload",
    bgcolor: "primary",
    route: "apps/document-upload",
  },
  {
    displayName: "EIN Filing",
    iconName: "tablerFileCertificate", // or 'tablerIdBadge2'
    bgcolor: "primary",
    route: "apps/ein-details-list",
  },
  {
    displayName: "Forum",
    iconName: "tablerEdit",
    bgcolor: "primary",
    route: "apps/forum",
  },
    {
    displayName: 'ToDo',
    iconName: 'tablerChecklist',
    bgcolor: 'primary',
    route: 'apps/todo',
  },
   {
    displayName: "Company Details",
    iconName: "tablerBuildingSkyscraper",
    bgcolor: "primary",
    route: "apps/companyDetails",
  },
  {
    displayName: "Business Hub",
    iconName: "tablerBuildingStore",
    bgcolor: "primary",
    route: "apps/business-hub",
  },
];
