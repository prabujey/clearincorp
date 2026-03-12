import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { UserModel } from "src/app/models/account-settings";
import { SecureStorageService } from "../services/storage/secure-storage.service";

@Injectable({
  providedIn: "root",
})
export class UserService {

  constructor(private secureStorage: SecureStorageService,){}

  private userSubject = new BehaviorSubject<UserModel | null>(
    this.getUserData()
  );

  user$ = this.userSubject.asObservable();

  private getUserData(): UserModel | null {
    const stored = this.secureStorage.getLoggedInUserData();
    return stored ? JSON.parse(stored) : null;
  }

  updateUser(user: UserModel) {
    this.secureStorage.setLoggedInUserData(JSON.stringify(user));
    this.userSubject.next(user);
  }

  getUserSnapshot(): UserModel | null {
    return this.userSubject.value;
  }
}
