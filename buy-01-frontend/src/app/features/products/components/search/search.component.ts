import { Component, EventEmitter, Output, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ProductService } from '../../services/product.service';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { Subject, of } from 'rxjs';
import { SearchVoiceComponent } from '../search-voice/search-voice.component';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SearchVoiceComponent],
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit, OnDestroy {
  @Output() search = new EventEmitter<string>();
  
  searchControl = new FormControl('');
  suggestions: string[] = [];
  showSuggestions = false;
  
  private productService = inject(ProductService);
  private destroy$ = new Subject<void>();

  ngOnInit() {
    // Handle suggestions while typing
    this.searchControl.valueChanges.pipe(
      debounceTime(300), // Wait 300ms after typing stops
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) {
          return of([]);
        }
        return this.productService.suggestProducts(query);
      }),
      takeUntil(this.destroy$)
    ).subscribe(suggestions => {
      this.suggestions = suggestions;
      this.showSuggestions = suggestions.length > 0;
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Triggered by Enter key or Search button
  onSearch() {
    this.showSuggestions = false;
    this.search.emit(this.searchControl.value || '');
  }

  // Triggered when clicking a suggestion
  selectSuggestion(suggestion: string) {
    this.searchControl.setValue(suggestion, { emitEvent: false });
    this.showSuggestions = false;
    this.search.emit(suggestion);
  }

  onBlur() {
    // Small delay to allow the click event on a suggestion to register before hiding
    setTimeout(() => {
      this.showSuggestions = false;
    }, 200);
  }

  onFocus() {
    if (this.suggestions.length > 0) {
      this.showSuggestions = true;
    }
  }

  onVoiceSearch(term: string) {
    this.searchControl.setValue(term);
    this.onSearch();
  }
}
