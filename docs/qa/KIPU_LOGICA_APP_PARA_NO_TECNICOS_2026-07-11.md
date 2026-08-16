# Kipu — Explicacion completa de la app para personas no tecnicas

Ultima revision: 11 de julio de 2026.  
Audiencia de este documento: personas que quieren entender que hace Kipu sin saber programacion.

---

## 1. Resumen corto

Kipu es una app Android de finanzas personales pensada para personas en Peru que manejan su dinero del dia a dia con Yape, Plin y efectivo.

La idea central es simple:

> Kipu ayuda a saber cuanto dinero tienes disponible, en que se esta yendo tu plata, cuanto puedes gastar en cada categoria, si estas haciendo muchos gastos pequenos, y que deudas, metas o gastos compartidos tienes pendientes.

La app no se conecta al banco ni pide claves. El usuario registra movimientos manualmente, comparte comprobantes de Yape/Plin o, si quiere, activa lectura de notificaciones para detectar ingresos. Aun cuando la app detecta algo, el usuario revisa y confirma antes de guardarlo como movimiento valido.

---

## 2. Para quien esta pensada

Kipu esta pensada para usuarios de Peru que:

- pagan o reciben dinero por Yape y Plin;
- usan efectivo en gastos diarios;
- quieren controlar gastos pequenos como snacks, taxis, delivery o compras rapidas;
- quieren separar su dinero por "sobres" o presupuestos;
- tienen metas como ahorro de emergencia, viaje, laptop o estudios;
- deben dinero a amigos/familiares o tienen pagos pendientes;
- hacen gastos compartidos en grupo, llamados "juntas" en la app;
- no quieren entregar claves bancarias ni conectar cuentas.

Ejemplo cotidiano:

> Una persona recibe S/ 120 por Yape, paga S/ 8 en desayuno, S/ 4 en pasaje, S/ 16 en delivery y S/ 10 en taxi. Kipu puede ayudarle a ver que queda para la semana, si se esta pasando en comida/transporte y si esos gastos pequenos ya forman un patron.

---

## 3. Que problema resuelve

Muchas personas no pierden control de sus finanzas por un solo gasto grande, sino por muchos gastos pequenos y dispersos:

- "solo S/ 5";
- "solo un taxi";
- "solo un delivery";
- "solo un Yape mas";
- "luego lo anoto".

Kipu intenta convertir ese caos diario en informacion clara:

- movimientos: entradas y salidas de dinero;
- sobres: limites por categoria;
- disponible: cuanto queda para el ciclo actual;
- alertas: avisos de gastos hormiga;
- compromisos: metas, deudas y pagos pendientes;
- juntas: gastos compartidos;
- exportacion: copia de tus datos;
- borrado total: eliminar datos locales.

---

## 4. Que NO hace Kipu

Esto es importante para entender su enfoque de seguridad:

- No pide clave bancaria.
- No pide PIN, token, contrasena ni acceso a banca movil.
- No entra a tu cuenta bancaria.
- No promete "Zero-Knowledge" ni seguridad criptografica que no exista.
- No necesita internet para el flujo principal.
- No sube comprobantes a la nube por defecto.
- No usa IA generativa para interpretar tus finanzas en el MVP.
- No guarda automaticamente un movimiento sugerido como definitivo sin revision humana.
- No fusiona duplicados en silencio.

Kipu funciona mas como una libreta inteligente local que como una app bancaria conectada.

---

## 5. Como imaginar la app

Una forma sencilla de imaginar Kipu:

### 5.1 Libreta de movimientos

Cada ingreso o gasto es un "movimiento".

Ejemplos:

- ingreso de S/ 120 por Yape;
- gasto de S/ 8 en comida;
- gasto de S/ 4 en transporte;
- ingreso manual por efectivo;
- pago compartido de una junta.

Cada movimiento puede tener:

- monto;
- tipo: ingreso o gasto;
- categoria;
- canal: efectivo, Yape, Plin u otro;
- origen: manual, comprobante, notificacion;
- estado: confirmado o pendiente de confirmar;
- fecha;
- contraparte, si existe;
- numero de operacion, si existe;
- vinculacion a una meta, si aplica.

### 5.2 Sobres

Un "sobre" es un presupuesto para una categoria.

Ejemplo:

- Comida: S/ 120 por semana.
- Transporte: S/ 50 por semana.
- Ocio: S/ 80 por semana.
- Gastos hormiga: S/ 35 por semana.

La app compara lo que gastas contra el limite de cada sobre.

### 5.3 Plan financiero

El plan financiero es la foto grande:

- cuanto ganas al mes;
- cuales son tus gastos fijos;
- cuanto quieres separar por sobres;
- si tienes metas;
- si tienes deudas sociales;
- cada cuanto quieres controlar tu presupuesto: diario, semanal o mensual.

Antes de guardar el plan, Kipu revisa si "cuadra": si el ingreso estimado alcanza para cubrir gastos fijos, sobres y compromisos.

### 5.4 Comprobantes

Si compartes una imagen de comprobante Yape o Plin con Kipu, la app intenta leerlo localmente.

Luego propone datos como:

- monto;
- canal: Yape o Plin;
- persona/comercio;
- numero de operacion;
- fecha;
- categoria sugerida.

Pero el usuario revisa antes de guardar.

### 5.5 Notificaciones

Si el usuario activa la opcion y concede permiso de Android, Kipu puede leer notificaciones de apps permitidas como Yape o Interbank/Plin.

Solo intenta detectar ingresos. Si ve una notificacion de pago saliente, la rechaza.

Lo detectado queda pendiente de confirmacion.

### 5.6 Juntas

Las juntas son grupos de gastos compartidos.

Ejemplo:

> Tres amigos salen a comer. Una persona paga S/ 90. Kipu puede registrar la junta, los participantes y calcular cuanto le toca a cada uno.

---

## 6. Pantallas principales

La app tiene una navegacion principal con estas secciones:

1. Inicio
2. Movimientos
3. Sobres
4. Compromisos
5. Perfil

Tambien tiene pantallas secundarias:

- onboarding o bienvenida inicial;
- wizard de plan financiero;
- comprobantes;
- revision de comprobante;
- juntas;
- politica de privacidad.

---

## 7. Primer uso de la app

Cuando alguien abre Kipu por primera vez:

1. La app revisa si el onboarding ya fue completado.
2. Si no fue completado, muestra la pantalla de bienvenida.
3. Al terminar, puede marcar que hay un plan pendiente.
4. Si hay plan pendiente, abre el wizard de plan financiero.
5. Luego entra al area principal de la app.

En palabras simples:

> Primero Kipu te recibe, luego te ayuda a preparar un plan basico y despues te muestra tu resumen financiero.

---

## 8. Inicio: que muestra y como piensa

La pantalla Inicio es el resumen de la situacion.

Puede mostrar:

- disponible del ciclo actual;
- resumen de gastos del periodo;
- alertas de gasto hormiga;
- ultimos movimientos;
- cantidad de movimientos;
- cantidad de sobres;
- estado general de flujo de dinero.

### 8.1 Que significa "disponible"

Disponible significa:

> cuanto dinero queda dentro de tus sobres para el ciclo actual.

Si el ciclo es semanal, mira la semana.  
Si el ciclo es mensual, mira el mes.  
Si el ciclo es diario, mira el dia.

Ejemplo:

- Sobres totales de la semana: S/ 250.
- Ya gastaste: S/ 100.
- Queda: S/ 150.
- Si quedan 5 dias, puede mostrar un disponible aproximado por dia o por ciclo, segun la configuracion.

### 8.2 De donde salen los datos del Inicio

Inicio combina varias fuentes:

- sobres;
- movimientos;
- compromisos;
- preferencias;
- plan financiero;
- fecha actual.

Esto es importante: Inicio no inventa datos. Calcula el resumen a partir de lo registrado localmente.

### 8.3 Ultimos movimientos

El Inicio muestra los movimientos confirmados mas recientes. Los movimientos pendientes no se tratan como definitivos.

---

## 9. Movimientos

La pantalla Movimientos es la lista de ingresos y gastos.

Un movimiento puede venir de:

- registro manual;
- comprobante Yape/Plin;
- notificacion Yape/Plin;
- gasto compartido;
- vinculacion a compromiso o meta.

### 9.1 Movimiento confirmado

Un movimiento confirmado ya cuenta para los calculos.

Ejemplo:

- gasto de S/ 12 en comida;
- ingreso de S/ 100 por Yape confirmado;
- gasto de S/ 25 en transporte.

### 9.2 Movimiento pendiente

Un movimiento pendiente necesita revision.

Ejemplo:

> Kipu lee una notificacion que parece decir que recibiste S/ 80 por Yape. No lo guarda como definitivo. Lo deja pendiente para que el usuario confirme o descarte.

### 9.3 Registro manual

El usuario puede crear movimientos manuales.

Debe elegir:

- tipo: ingreso o gasto;
- monto;
- categoria;
- canal;
- descripcion opcional;
- contraparte opcional.

Kipu valida que:

- el monto sea valido;
- el monto no sea cero;
- exista categoria;
- el movimiento manual no quede como pendiente.

### 9.4 Filtros

La pantalla puede filtrar por canal o categoria.

Ejemplo:

- ver solo Yape;
- ver solo Plin;
- ver solo comida;
- ver solo transporte.

### 9.5 Cambio de categoria

Si un gasto fue clasificado mal, el usuario puede cambiar la categoria.

Ejemplo:

> Un gasto detectado como "Otros" puede cambiarse a "Comida".

### 9.6 Vincular ingreso a una meta

Un ingreso puede vincularse a una meta de ahorro.

Ejemplo:

> Recibi S/ 50 y quiero que cuente como avance para mi meta "Fondo de emergencia".

Esa vinculacion ayuda a calcular el progreso de la meta.

---

## 10. Duplicados

Kipu intenta detectar posibles movimientos duplicados.

Esto importa porque un mismo pago puede aparecer de varias formas:

- el usuario lo registro manualmente;
- luego compartio un comprobante;
- luego llego una notificacion;
- o se intento importar otra vez.

### 10.1 Que hace Kipu ante un duplicado

Kipu no decide solo. Muestra opciones al usuario.

Opciones posibles:

- fusionar;
- guardar como nuevo;
- cancelar;
- descartar alerta.

### 10.2 Por que no fusiona solo

Porque en finanzas personales un falso duplicado puede causar perdida de informacion.

Ejemplo:

> Dos pagos de S/ 20 al mismo comercio pueden parecer duplicados, pero tal vez fueron dos compras reales.

Por eso la confirmacion humana es una regla de producto.

---

## 11. Sobres

Los sobres son presupuestos por categoria.

Ejemplo:

- Comida: S/ 120.
- Transporte: S/ 50.
- Ocio: S/ 80.
- Familia: S/ 100.
- Gastos hormiga: S/ 35.

### 11.1 Como calcula un sobre

Para cada sobre, Kipu calcula:

- limite;
- gastado;
- restante;
- porcentaje usado;
- estado.

Ejemplo:

- Limite de comida: S/ 120.
- Gastaste: S/ 96.
- Queda: S/ 24.
- Usaste: 80%.
- Estado: cerca del limite.

### 11.2 Estados del sobre

Un sobre puede estar:

- OK: gasto dentro de rango.
- Cerca del limite: cuando llega aproximadamente a 80%.
- Excedido: cuando gastaste mas que el limite.

### 11.3 De donde sale el gasto del sobre

El gasto de un sobre sale de movimientos confirmados de esa categoria dentro del ciclo actual.

Ejemplo:

> Si un sobre es "Comida", Kipu suma los gastos confirmados en categoria Comida durante la semana o ciclo actual.

### 11.4 Crear y eliminar sobres

El usuario puede crear sobres nuevos, asignarlos a categorias y definir limites.

Tambien puede eliminarlos.

### 11.5 Ajustar limite

Si el usuario quiere cambiar el monto del sobre, puede editarlo.

Ejemplo:

> Subir Transporte de S/ 50 a S/ 70.

---

## 12. Gastos hormiga

Los gastos hormiga son gastos pequenos que, sumados, pesan bastante.

Ejemplos:

- S/ 4 en snack;
- S/ 8 en taxi corto;
- S/ 7 en cafe;
- S/ 10 en delivery;
- S/ 5 en antojo.

### 12.1 Que detecta Kipu

Kipu mira gastos pequenos dentro de una ventana de tiempo.

Si hay varios gastos pequenos en la misma categoria, puede levantar una alerta.

### 12.2 Por que algunas categorias se excluyen

Si un sobre esta saludable, Kipu evita alertar innecesariamente para esa categoria.

Ejemplo:

> Si Transporte esta muy por debajo de su limite, varios gastos pequenos en transporte pueden no ser tan preocupantes.

### 12.3 Alerta por limite semanal

Ademas del patron de varios gastos, Kipu tambien puede revisar si el sobre de gastos hormiga llego a cierto porcentaje del limite.

Por defecto, el umbral usado en preferencias es 80%.

---

## 13. Compromisos

Los compromisos son cosas que el usuario tiene pendientes con su dinero.

Hay tres tipos principales:

1. Meta de ahorro.
2. Deuda social.
3. Pago pendiente.

### 13.1 Meta de ahorro

Ejemplo:

- Fondo de emergencia.
- Laptop.
- Viaje.
- Estudios.

Una meta tiene:

- nombre;
- monto objetivo;
- monto actual;
- horizonte en meses;
- moneda.

Kipu puede sugerir cuanto ahorrar por semana o mes para llegar.

### 13.2 Deuda social

Una deuda social es dinero que debes a otra persona.

Ejemplo:

> Le debo S/ 80 a Ana.

Puede guardar:

- persona;
- monto;
- estado pendiente o saldado.

### 13.3 Pago pendiente

Un pago pendiente es una obligacion que aun no se cerro.

Ejemplo:

> Tengo que pagar un servicio o una cuota.

### 13.4 Progreso de metas con ingresos vinculados

Si vinculas un ingreso a una meta, Kipu puede contar ese ingreso como avance.

Ejemplo:

- Meta: S/ 500.
- Ya tenia: S/ 100.
- Vinculo ingreso de S/ 50.
- Progreso considerado: S/ 150.

---

## 14. Plan financiero

El plan financiero es uno de los centros de la app.

Sirve para responder:

> Con lo que gano, mis gastos fijos, mis sobres y mis metas, mi plan realmente alcanza?

### 14.1 Que datos pide

El wizard puede trabajar con:

- tipo de ingreso;
- frecuencia de pago;
- ingreso fijo;
- ingreso aproximado;
- ingresos variables;
- ingresos adicionales;
- saldo inicial;
- gastos fijos;
- sobres;
- limite de gastos hormiga;
- categorias de gastos hormiga;
- meta de ahorro;
- deuda social;
- ciclo de presupuesto.

El saldo inicial representa la plata disponible al empezar a usar el plan. Inicio lo incorpora en “Efectivo real”: saldo inicial + ingresos confirmados - gastos confirmados.

### 14.2 Tipo de ingreso

Puede haber perfiles como:

- ingreso fijo;
- ingreso variable;
- aproximado.

La app intenta estimar el ingreso mensual a partir de lo que el usuario llena.

### 14.3 Frecuencia de pago

Ejemplos:

- mensual;
- quincenal;
- semanal;
- variable.

Esto ayuda a convertir el ingreso a una vista mensual.

### 14.4 Gastos fijos

La app puede pedir gastos como:

- educacion;
- alquiler;
- servicios;
- telefono;
- deudas;
- otros gastos personalizados.

Tambien permite saltarse esta seccion.

### 14.5 Sobres del plan

El wizard propone sobres base:

- Comida;
- Transporte;
- Ocio;
- Familia;
- Gastos hormiga.

Tambien permite sobres personalizados.

### 14.6 Validacion del plan

Antes de guardar, Kipu calcula:

```text
ingreso mensual estimado
- gastos fijos
- reserva mensual para sobres
- carga mensual de compromisos
= sobrante o deficit
```

Si el resultado es negativo, el plan no se guarda.

Ejemplo:

- Ingreso mensual: S/ 1,500.
- Gastos fijos: S/ 900.
- Sobres proyectados: S/ 500.
- Meta/deudas: S/ 200.
- Total requerido: S/ 1,600.
- Deficit: S/ 100.

En ese caso Kipu dira que el plan no cuadra y pedira ajustar montos.

### 14.7 Ciclo de presupuesto

El plan puede trabajar por ciclo:

- diario;
- semanal;
- mensual.

Ese ciclo afecta los calculos de disponible y sobres.

---

## 15. Comprobantes Yape y Plin

Kipu permite registrar pagos a partir de imagenes de comprobantes.

### 15.1 Como llega un comprobante

El usuario comparte una imagen hacia Kipu desde Android.

Ejemplo:

> Desde la galeria o desde otra app, el usuario elige "Compartir" y selecciona Kipu.

### 15.2 Que hace Kipu con la imagen

1. Abre la imagen localmente.
2. La prepara para OCR.
3. Usa ML Kit local para leer texto.
4. Limpia el texto.
5. Busca senales de Yape o Plin.
6. Extrae datos.
7. Muestra pantalla de revision.

### 15.3 Que datos intenta leer

Puede intentar detectar:

- monto;
- contraparte;
- mensaje;
- numero de operacion;
- fecha y hora;
- canal: Yape o Plin.

### 15.4 Como decide la confianza

La confianza sube si hay senales fuertes, por ejemplo:

- numero de operacion;
- fecha y hora;
- monto;
- contraparte.

Pero incluso con alta confianza, el usuario revisa.

### 15.5 Que pasa si no reconoce el comprobante

Si no reconoce Yape ni Plin:

- no inventa un resultado;
- muestra advertencia;
- permite completar datos manualmente.

### 15.6 Que tipo de movimiento crea

Los comprobantes compartidos se tratan como gastos, porque normalmente representan pagos hechos por el usuario.

---

## 16. Notificaciones de Yape y Plin

La lectura de notificaciones es opcional.

### 16.1 Que necesita para funcionar

El usuario debe:

1. activar la preferencia en Kipu;
2. conceder permiso de acceso a notificaciones en Android.

Si no hace eso, Kipu no procesa notificaciones.

### 16.2 Que apps observa

Kipu solo considera paquetes permitidos:

- Yape;
- Interbank, usado para Plin;
- alias antiguos mantenidos por compatibilidad.

No procesa notificaciones de cualquier app.

### 16.3 Que tipo de notificacion acepta

Solo busca ingresos.

Acepta textos parecidos a:

- "te yapearon";
- "recibiste";
- "te envio";
- "recibiste un plin".

Rechaza pagos salientes como:

- "enviaste";
- "pagaste";
- "pagaste con Plin".

### 16.4 Que pasa cuando detecta un ingreso

Lo guarda como movimiento pendiente de confirmacion.

Eso significa:

> Kipu cree que puede haber un ingreso, pero espera que el usuario lo confirme.

### 16.5 Confirmacion humana

El usuario puede:

- confirmar;
- descartar;
- resolver si parece duplicado.

Kipu no auto-aprueba ingresos detectados por notificacion en el codigo actual.

---

## 17. Juntas

Las juntas son cuentas compartidas.

### 17.1 Que puede hacer una junta

Permite:

- crear una junta;
- poner participantes;
- editar la junta;
- borrar una junta;
- registrar un gasto compartido;
- vincular un movimiento existente;
- calcular resumen de quien pago y quien debe.

Una persona que ya figura como pagadora de un gasto no puede quitarse ni renombrarse desde la edición, porque eso rompería la liquidación. Para cambiarla primero debe resolverse el historial asociado.

Al borrar una junta, Kipu pide confirmación y avisa que también eliminará todos sus gastos compartidos. El borrado no ocurre al tocar directamente la opción del menú.

### 17.2 Ejemplo

Tres personas comparten una comida:

- Total: S/ 90.
- Participantes: Ana, Luis y Carla.
- Pago: Ana.
- A cada uno le toca: S/ 30.
- Luis debe S/ 30 a Ana.
- Carla debe S/ 30 a Ana.

La app ayuda a ordenar esa informacion.

---

## 18. Perfil

Perfil concentra preferencias, privacidad y datos.

### 18.1 Tema

El usuario puede cambiar modo visual:

- sistema;
- claro;
- oscuro.

### 18.2 Notificaciones

Puede activar o desactivar deteccion de ingresos por notificaciones.

Si activa y no tiene permiso, la app muestra una explicacion y abre ajustes de Android.

### 18.3 Exportar datos

Kipu permite exportar datos.

Formatos:

- JSON: exportacion completa mas estructurada.
- CSV: tabla de movimientos.
- CSV compatible con Excel Peru: pensado para separadores regionales.

Antes de exportar, muestra advertencia.

### 18.4 Borrar todos los datos

El borrado es intencionalmente cuidadoso:

1. primer dialogo de confirmacion;
2. segundo dialogo final;
3. limpia preferencias;
4. borra tablas locales;
5. limpia archivos temporales de exportacion y recibos;
6. resembra categorias base.

Despues del wipe quedan categorias base para que la app pueda seguir funcionando:

- Comida;
- Transporte;
- Servicios;
- Otros.

---

## 19. Exportacion: que datos incluye

La exportacion completa toma una foto local de:

- movimientos;
- categorias;
- sobres;
- compromisos;
- planes financieros;
- juntas;
- gastos de juntas;
- pares de duplicados descartados;
- preferencias;
- fecha de exportacion.

El archivo se crea en cache local y luego se comparte usando el sistema de Android.

---

## 20. Borrado de datos: que elimina

El borrado total elimina:

- movimientos;
- duplicados descartados;
- compromisos;
- planes financieros;
- sobres;
- juntas;
- gastos de juntas;
- categorias;
- preferencias;
- caches locales de exportacion;
- caches locales de recibos.

Luego vuelve a crear solo las categorias base.

---

## 21. Datos principales que maneja Kipu

### 21.1 Movimiento

Representa una entrada o salida de dinero.

Contiene:

- identificador;
- tipo: ingreso o gasto;
- monto;
- categoria;
- canal;
- origen;
- estado;
- descripcion;
- contraparte;
- numero de operacion;
- meta vinculada;
- fecha del movimiento;
- fecha de creacion.

### 21.2 Categoria

Sirve para clasificar movimientos.

Categorias base:

- Comida;
- Transporte;
- Servicios;
- Otros.

Tambien puede haber categorias creadas por el usuario.

### 21.3 Sobre

Un presupuesto asociado a una categoria.

Contiene:

- nombre;
- limite;
- categoria asociada.

El gasto del sobre se calcula a partir de movimientos, no se escribe manualmente.

### 21.4 Compromiso

Meta, deuda o pago pendiente.

Contiene:

- tipo;
- titulo;
- monto objetivo;
- monto actual;
- persona relacionada, si aplica;
- si esta saldado;
- moneda;
- horizonte de ahorro.

### 21.5 Plan financiero

Contiene:

- ingreso mensual estimado;
- gastos fijos;
- saldo inicial;
- sobres incluidos;
- perfil de ingreso;
- frecuencia de pago;
- ciclo de presupuesto.

### 21.6 Junta

Contiene:

- nombre;
- participantes;
- si esta saldada.

### 21.7 Preferencias

Contiene:

- tema;
- notificaciones activas o no;
- onboarding completado o no;
- wizard de plan pendiente;
- limite de gastos hormiga;
- alerta de gastos hormiga;
- categorias vigiladas para gastos hormiga;
- texto del widget;
- ciclo de presupuesto.

---

## 22. Donde se guardan los datos

Kipu usa dos almacenes locales:

### 22.1 Base de datos local

Usa Room, que es una forma comun en Android de guardar datos estructurados en el dispositivo.

La base se llama:

```text
kipu.db
```

Guarda:

- movimientos;
- categorias;
- sobres;
- compromisos;
- planes;
- juntas;
- gastos de juntas;
- duplicados descartados.

### 22.2 Preferencias locales

Usa DataStore, pensado para configuraciones.

Guarda:

- tema;
- notificaciones;
- onboarding;
- gasto hormiga;
- widget;
- preferencias generales.

---

## 23. Como protege la privacidad

La app sigue varias reglas de producto y codigo:

- procesa datos financieros en el dispositivo;
- no requiere cuenta bancaria;
- no pide credenciales;
- no sube imagenes por defecto;
- OCR local;
- notificaciones opcionales;
- revision humana;
- exportacion voluntaria;
- borrado total disponible;
- componentes sensibles con validaciones;
- no debe loguear montos, nombres ni texto completo de OCR/notificaciones.

---

## 24. Como se organiza por dentro, explicado sin programacion

La app esta separada como una empresa pequena con areas:

### 24.1 Pantallas

Son lo que ve el usuario:

- Inicio;
- Movimientos;
- Sobres;
- Compromisos;
- Perfil;
- Recibos;
- Juntas;
- Plan.

### 24.2 Cerebro de cada pantalla

Cada pantalla tiene una pieza que prepara la informacion y responde a botones.

Ejemplo:

- si el usuario toca "guardar movimiento", valida datos y pide guardar;
- si toca "confirmar notificacion", manda confirmar;
- si toca "exportar", prepara el archivo.

En el codigo esto se llama ViewModel, pero para una persona no tecnica puede imaginarse como el coordinador de la pantalla.

### 24.3 Reglas financieras

Las reglas importantes viven separadas.

Ejemplos:

- calcular disponible;
- detectar gasto hormiga;
- validar plan;
- encontrar duplicados;
- confirmar comprobantes;
- guardar metas.

Esto evita que una pantalla tenga calculos escondidos.

### 24.4 Almacenamiento

Otra parte se encarga de guardar y leer datos del telefono.

Las pantallas no hablan directamente con la base de datos; piden datos a traves de contratos.

### 24.5 Diseno

Hay componentes visuales compartidos para que la app se vea consistente.

---

## 25. Ejemplos completos

### 25.1 Registrar un gasto manual

1. El usuario entra a Movimientos.
2. Toca agregar.
3. Elige gasto.
4. Ingresa S/ 12.
5. Elige categoria Comida.
6. Elige canal Efectivo.
7. Guarda.
8. El movimiento queda confirmado.
9. El sobre Comida suma S/ 12.
10. Inicio recalcula disponible.

### 25.2 Compartir comprobante Yape

1. El usuario comparte imagen del comprobante hacia Kipu.
2. Kipu abre revision de comprobante.
3. Lee imagen con OCR local.
4. Detecta Yape.
5. Extrae monto y contraparte.
6. Sugiere categoria si puede.
7. Muestra los campos al usuario.
8. El usuario revisa y corrige si hace falta.
9. Confirma.
10. Kipu revisa duplicados.
11. Si no hay problema, guarda el gasto.

### 25.3 Detectar ingreso por notificacion

1. Usuario activa notificaciones en Perfil.
2. Usuario concede permiso de Android.
3. Llega notificacion de Yape.
4. Kipu verifica que sea app permitida.
5. Kipu verifica que preferencia este activa.
6. Combina titulo y texto.
7. Busca senales de ingreso.
8. Rechaza si parece pago enviado.
9. Si parece ingreso, crea movimiento pendiente.
10. Usuario lo confirma o descarta en Movimientos.

### 25.4 Crear plan financiero

1. Usuario completa ingreso mensual.
2. Define gastos fijos.
3. Define sobres.
4. Define limite de gastos hormiga.
5. Define meta o la salta.
6. Indica si tiene deuda social.
7. Kipu calcula si el plan alcanza.
8. Si hay deficit, pide ajustar.
9. Si cuadra, guarda plan, sobres, preferencias y compromisos.

### 25.5 Borrar todos los datos

1. Usuario entra a Perfil.
2. Toca borrar datos.
3. Ve primera advertencia.
4. Confirma.
5. Ve confirmacion final.
6. Confirma.
7. Kipu borra preferencias, movimientos, sobres, planes, juntas, compromisos y caches.
8. Kipu vuelve a crear categorias base.

---

## 26. Palabras tecnicas traducidas

### ViewModel

Coordinador de una pantalla. Prepara datos y responde a acciones.

### UseCase

Regla o tarea importante. Por ejemplo: "calcular disponible" o "confirmar comprobante".

### Repository

Puente para leer o guardar datos sin que la pantalla sepa como se almacenan.

### Room

Base de datos local dentro del telefono.

### DataStore

Almacen local de preferencias.

### OCR

Tecnologia que lee texto dentro de una imagen.

### ML Kit

Herramienta local de Google usada para OCR.

### Pendiente de confirmacion

Dato sugerido por la app que aun no debe contar como definitivo.

### Confirmado

Dato revisado y aceptado por el usuario.

### Gasto hormiga

Gasto pequeno que parece inofensivo, pero al repetirse afecta el presupuesto.

---

## 27. Reglas de oro de Kipu

1. El usuario tiene la ultima palabra.
2. Los datos financieros se procesan localmente.
3. No se piden claves bancarias.
4. Notificaciones son opcionales.
5. Comprobantes se revisan antes de guardar.
6. Duplicados se resuelven con confirmacion humana.
7. El usuario puede exportar sus datos.
8. El usuario puede borrar sus datos.
9. Los calculos financieros viven fuera de las pantallas.
10. La app esta pensada para el contexto peruano.

---

## 28. Estado general observado

Segun el codigo y los documentos del repositorio, Kipu ya tiene implementados los flujos principales del MVP y trabajo posterior:

- onboarding;
- plan financiero;
- movimientos manuales;
- sobres;
- gastos hormiga;
- compromisos;
- comprobantes Yape/Plin;
- notificaciones opcionales;
- duplicados;
- exportacion;
- borrado total;
- juntas;
- politica de privacidad;
- preparacion para pruebas internas de Play Store.

La siguiente actividad humana indicada por la documentacion del proyecto es continuar con Play Console internal testing.

---

## 29. Lectura rapida en una frase

Kipu es una libreta financiera local para Peru que ayuda a registrar ingresos y gastos, leer comprobantes, controlar sobres, detectar gastos hormiga, manejar metas/deudas/juntas y mantener control de datos sin pedir claves bancarias.
