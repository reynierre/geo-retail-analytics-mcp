import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'app-spinner',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div [class]="containerClass()">
      <div [class]="spinnerClass()"></div>
    </div>
  `,
  styles: [`
    .spinner {
      border: 2px solid #f3f4f6;
      border-top-color: #3b82f6;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class SpinnerComponent {
  readonly size = input<'small' | 'medium' | 'large'>('medium');

  readonly containerClass = () => {
    return 'flex items-center justify-center';
  };

  readonly spinnerClass = () => {
    const baseClass = 'spinner';
    const sizeMap = {
      small: 'w-4 h-4',
      medium: 'w-8 h-8',
      large: 'w-12 h-12'
    };
    return `${baseClass} ${sizeMap[this.size()]}`;
  };
}
