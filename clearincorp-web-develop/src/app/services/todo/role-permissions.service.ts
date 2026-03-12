import { Injectable } from "@angular/core";
import { User, TaskPermissions } from "src/app/models/todo-models/todo.model";

@Injectable({
  providedIn: "root",
})
export class RolePermissionsService {
  getTaskPermissions(
    user: User,
    taskType: "personal" | "assigned",
    isTaskOwner: boolean = false,
    isAssignedToUser: boolean = false
  ): TaskPermissions {
    const { role } = user;

    if (taskType === "personal") {
      return {
        canCreate: true,
        canEdit: true,
        canDelete: true,
        canAssign: false,
        canMarkDone: true,
        canIgnore: true,
      };
    }
    console.log(role,"Role in Roleperservive");
    
    // Assigned tasks permissions
    switch (role) {
      case "Admin":
        return {
          canCreate: true,
          canEdit: isTaskOwner,
          canDelete: isTaskOwner,
          canAssign: true,
          canMarkDone: isAssignedToUser,
          canIgnore: isAssignedToUser,
        };

      case "SuperFiler":
        return {
          canCreate: true,
          canEdit: isTaskOwner,
          canDelete: isTaskOwner,
          canAssign: true,
          canMarkDone: isAssignedToUser,
          canIgnore: isAssignedToUser,
        };

      case "Vendor":
        return {
          canCreate: false, // Vendor can only create PERSONAL tasks
          canEdit: false,
          canDelete: false,
          canAssign: false,
          canMarkDone: isAssignedToUser,
          canIgnore: isAssignedToUser,
        };

      case "Consumer":
        return {
          canCreate: false, // Consumer can only create PERSONAL tasks
          canEdit: false,
          canDelete: false,
          canAssign: false,
          canMarkDone: isAssignedToUser,
          canIgnore: isAssignedToUser,
        };

      default:
        return {
          canCreate: false,
          canEdit: false,
          canDelete: false,
          canAssign: false,
          canMarkDone: false,
          canIgnore: false,
        };
    }
  }

  getAssignableRoles(userRole: User["role"]): User["role"][] {
    switch (userRole) {
      case "Admin":
        return ["Superfiler", "Vendor"];
      case "Superfiler":
        return ["Consumer"];
      default:
        return [];
    }
  }
}
