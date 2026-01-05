# Guía de Desarrollo - Frontend Angular 21

## Verificación de la Instalación

```bash
# Verificar versiones instaladas
npm list --depth=0 | grep -E "(angular|typescript|tailwind)"

# Debería mostrar:
# - @angular/core@21.x
# - typescript@5.9.x
# - tailwindcss@4.x
```

## Desarrollo

### Iniciar el servidor de desarrollo

```bash
npm start
# o
npx ng serve
```

El servidor estará disponible en `http://localhost:4200/`

Los cambios se recargarán automáticamente cuando edites archivos.

### Proxy al Backend

El archivo `proxy.conf.json` está configurado para redirigir todas las peticiones `/api/*` al backend en `http://localhost:8081`.

**Asegúrate de que el backend Spring Boot esté corriendo antes de iniciar el frontend.**

## Build

### Build de Desarrollo

```bash
npm run build
# Salida en: dist/geo-retail-analytics-frontend/
```

### Build de Producción

```bash
npx ng build --configuration=production
```

Optimizaciones aplicadas:
- Minificación
- Tree-shaking
- Lazy loading
- Bundle size optimizado (~82 KB gzipped inicial)

## Estructura del Código

### Componentes

Los componentes siguen el patrón Angular 21 sin el sufijo `.component`:

```
src/app/features/chat/
├── chat.ts                    # Componente principal
├── components/
│   ├── chat-input.ts          # Input del chat
│   └── chat-message.ts        # Mensaje individual
├── services/
│   ├── chat.service.ts        # Estado del chat
│   └── streaming.service.ts   # SSE streaming
└── models/
    └── message.model.ts       # Interfaces TypeScript
```

### Servicios

Los servicios usan Signals para gestión de estado:

**chat.service.ts:**
- Gestiona el estado global del chat (mensajes, loading, errores)
- Usa un `signal<ChatState>` privado
- Expone `computed()` signals para selectores

**streaming.service.ts:**
- Maneja la conexión SSE con el backend
- Usa fetch API nativa
- Procesa eventos Server-Sent en tiempo real

### Estilos

**Tailwind CSS 4:**
- Configurado en `postcss.config.js`
- Importado en `src/styles.css` con `@import "tailwindcss"`
- Utility-first approach
- Purga automática de clases no utilizadas en producción

## Patrones Angular 21 Usados

### 1. Zoneless Change Detection

```typescript
// app.config.ts
providers: [
  provideZonelessChangeDetection()
]
```

Beneficios:
- Mejor performance
- Bundle más pequeño (no incluye zone.js en runtime)
- Detección de cambios más predecible

### 2. Signals para Estado

```typescript
// Uso de signals
const count = signal(0);
const doubled = computed(() => count() * 2);

// Actualizar
count.set(5);
count.update(n => n + 1);
```

### 3. Control Flow Moderno

```html
<!-- Condicionales -->
@if (condition) {
  <div>Content</div>
} @else if (other) {
  <div>Other</div>
} @else {
  <div>Default</div>
}

<!-- Loops (SIEMPRE usar track) -->
@for (item of items(); track item.id) {
  <div>{{ item.name }}</div>
} @empty {
  <p>No items</p>
}
```

### 4. input() y output()

```typescript
export class MyComponent {
  // Reemplaza @Input()
  readonly data = input.required<DataType>();
  readonly optional = input('default value');

  // Reemplaza @Output()
  readonly clicked = output<void>();
  readonly selected = output<Item>();
}
```

### 5. inject() Function

```typescript
export class MyComponent {
  // Reemplaza constructor injection
  private readonly myService = inject(MyService);
  private readonly router = inject(Router);
}
```

## Testing (Preparado para el futuro)

El proyecto está configurado para usar Karma/Jasmine. Para ejecutar tests:

```bash
npm test
```

Patrón de test recomendado:

```typescript
describe('ChatComponent', () => {
  it('should create', () => {
    const fixture = TestBed.createComponent(ChatComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should add message when sendMessage is called', () => {
    const service = TestBed.inject(ChatService);
    service.addMessage({ id: '1', role: 'user', content: 'test', timestamp: new Date() });
    expect(service.messages().length).toBe(1);
  });
});
```

## Troubleshooting

### Error: "Could not resolve @shared/..."

**Solución:** Asegúrate de que `tsconfig.json` tenga `"baseUrl": "."` configurado.

### Error: TypeScript version mismatch

**Solución:** El proyecto requiere TypeScript 5.9+. Actualiza package.json:
```json
"typescript": "~5.9.2"
```

### Build falla con errores de Tailwind

**Solución:** Verifica que `postcss.config.js` esté configurado correctamente:
```javascript
export default {
  plugins: {
    '@tailwindcss/postcss': {}
  }
};
```

### SSE no funciona

**Solución:**
1. Verifica que el backend esté corriendo en puerto 8081
2. Revisa `proxy.conf.json`
3. Abre DevTools > Network > busca peticiones a `/api/chat/stream`

## Comandos Útiles

```bash
# Generar nuevo componente
npx ng generate component features/nueva-feature

# Generar servicio
npx ng generate service features/nueva-feature/services/mi-servicio

# Analizar bundle size
npx ng build --stats-json
npx webpack-bundle-analyzer dist/geo-retail-analytics-frontend/stats.json

# Linting (cuando esté configurado)
npx ng lint
```

## Performance Tips

1. **Lazy Loading:** Los componentes de rutas ya usan `loadComponent()`
2. **OnPush:** Todos los componentes usan `ChangeDetectionStrategy.OnPush`
3. **Signals:** Reducen re-renders innecesarios
4. **track en @for:** Optimiza rendering de listas

## Próximos Pasos

- [ ] Configurar tests unitarios
- [ ] Agregar manejo de errores más robusto
- [ ] Implementar persistencia de conversación (localStorage)
- [ ] Agregar soporte para Markdown en mensajes
- [ ] Mejorar UX con animaciones
- [ ] Agregar tema oscuro

## Recursos

- [Angular 21 Docs](https://angular.dev)
- [Signals Guide](https://angular.dev/guide/signals)
- [Zoneless Guide](https://angular.dev/guide/zoneless)
- [Tailwind CSS 4](https://tailwindcss.com/docs)
