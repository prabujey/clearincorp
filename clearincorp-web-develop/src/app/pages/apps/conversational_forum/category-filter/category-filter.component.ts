// import { Component, EventEmitter, Output } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { Category } from 'src/app/models/forum.model';

// @Component({
//   selector: 'app-category-filter',
//   standalone: true,
//   imports: [CommonModule],
//   template: `
//     <div class="category-filter">
//       <div class="category-scroll">
//         <button
//           class="category-chip"
//           [class.active]="selectedCategory === null"
//           (click)="selectCategory(null)"
//         >
//           All Topics
//         </button>
//         <button
//           *ngFor="let category of categories"
//           class="category-chip"
//           [class.active]="selectedCategory === category"
//           (click)="selectCategory(category)"
//         >
//           {{ category }}
//         </button>
//       </div>
//     </div>
//   `,
//   styles: [`
//     .category-filter {
//       margin-bottom: 1.5rem;
//       overflow-x: auto;
//       -webkit-overflow-scrolling: touch;
//     }

//     .category-filter::-webkit-scrollbar {
//       display: none;
//     }

//     .category-scroll {
//       display: flex;
//       gap: 0.5rem;
//       min-width: min-content;
//     }

//     .category-chip {
//       padding: 0.5rem 1rem;
//       border: 1px solid #e5e7eb;
//       background: white;
//       color: #6b7280;
//       border-radius: 9999px;
//       font-size: 0.875rem;
//       font-weight: 500;
//       white-space: nowrap;
//       cursor: pointer;
//       transition: all 0.2s;
//     }

//     .category-chip:hover {
//       border-color: #2563eb;
//       color: #2563eb;
//       background: #eff6ff;
//     }

//     .category-chip.active {
//       background: #2563eb;
//       color: white;
//       border-color: #2563eb;
//     }

//     @media (max-width: 640px) {
//       .category-filter {
//         margin-left: -1rem;
//         margin-right: -1rem;
//         padding-left: 1rem;
//         padding-right: 1rem;
//       }
//     }
//   `]
// })
// export class CategoryFilterComponent {
//   @Output() categorySelected = new EventEmitter<string | null>();

//   categories: Category[] = ['General', 'LLC Formation', 'Tax & Legal', 'Business Growth', 'Success Stories'];
//   selectedCategory: string | null = null;

//   selectCategory(category: string | null): void {
//     this.selectedCategory = category;
//     this.categorySelected.emit(category);
//   }
// }
import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Category } from 'src/app/models/forum.model';

@Component({
  selector: 'app-category-filter',
  standalone: true,
  imports: [CommonModule],
  templateUrl: `./category-filter.component.html`,
  styleUrls: [`./category-filter.component.scss`]
})
export class CategoryFilterComponent {
  @Output() categorySelected = new EventEmitter<string | null>();

  categories: Category[] = ['General', 'LLC Formation', 'Tax & Legal', 'Business Growth', 'Success Stories'];
  selectedCategory: string | null = null;

  selectCategory(category: string | null): void {
    this.selectedCategory = category;
    this.categorySelected.emit(category);
  }
}
