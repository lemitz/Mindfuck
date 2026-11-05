# Mindfuck — контекст для нового агента

**Корень проекта:** `Z:\opencode\Mindfuck`. Android 14+ (minSdk 34, compileSdk 37), Kotlin, Jetpack Compose (Material 3), Room. Сборка: AGP 9 со встроенным Kotlin (плагина `org.jetbrains.kotlin.android` в модуле нет; в корне он лежит `apply false` только чтобы задать версию Kotlin), Gradle 9, KSP для Room. Аналог eMoods: ежедневный лог настроения и симптомов, календарь, графики и PDF-отчёт на русском. Начните с [README.md](README.md).

## Непереговорные требования пользователя

- **Интерфейс на английском, PDF-отчёт всегда на русском.** Базовый отчёт повторяет бумажный образец «Дневник настроения и симптомов»: шкала 0-3 (белая / светло-серая / серая / чёрная клетка), таблица по дням с днями недели, «Краткое описание по периодам» с правильными склонениями («не отмечалась» / «не отмечалось»), «Записи самонаблюдения за <дату>», «стр. N». За 7 дней с заметками отчёт помещается на одну страницу. Тексты проверяют тесты в `app/src/test/.../RuTextTest.kt`: формат сверен с исходным образцом, данные в тестах выдуманные; не ломать.
- **Перед генерацией отчёта всегда диалог с галочками** дополнительных разделов (сон, да/нет, лекарства, свои пункты, графики, заметки за все дни). Выбор запоминается.
- **Имя файла:** `mindfuck_ДДММГГ.pdf`, следующие за тот же день `_v2`, `_v3`…; счётчик не откатывается назад. Копия в `Downloads/Mindfuck` через MediaStore.
- **Шрифт Realme Real Choice везде** (интерфейс, графики, PDF): `res/font/realchoice.ttf`, вариативный, `wght` 550 = Regular, 700 = Medium. Compose через `RealChoice`, Canvas/PDF через `AppFonts.typeface()`.
- **Тёмная тема по умолчанию**, светлая — переключателем в Settings (`ThemeSettings`), от системной темы не зависит.
- **Нативные 120 Гц:** `MainActivity.requestHighestRefreshRate()`; тяжёлое не делать в отрисовке, слайдер пишет в БД только по отпусканию. Без напоминаний и без биометрии (пользователь отказался).
- **Свайп между вкладками** по схеме FeatherW8: `HorizontalPager` с `beyondViewportPageCount = 1`, `tab` — единственный источник правды, `snapshotFlow { settledPage }` → `tab`, `LaunchedEffect(tab)` → `animateScrollToPage`, сравнение с `targetPage`, а не `settledPage`.
- **Дозы лекарств по дням:** `MedIntake.dose` — снимок дозы на момент отметки, правится для одного дня; смена дозы по умолчанию не переписывает прошлое.
- **Импорт eMoods только сливает, никогда не стирает** (`data/EmoodsImport.kt`, Settings → Import from eMoods): существующие значения дня главнее, заполняются только пустые поля, заметки дописываются построчно, лекарства сопоставляются по имени, повторный импорт ничего не меняет, перед импортом JSON-бэкап в `Downloads/Mindfuck`. Шкалы eMoods сдвинуты: 0 = не отмечено, 1..4 = None..Severe (наши 0..3); да/нет: 1 = нет, 2 = да; `TOOK` 2 = принято. Сопоставление проверено на реальной выгрузке пользователя; тесты (`EmoodsImportTest`) на выдуманных данных.
- **Никаких сетевых разрешений.** Данные медицинские: в README, скриншоты и тесты попадают только выдуманные данные. Реальный образец отчёта пользователя (с его заметками) в репозиторий не класть.

## Порядок работы

1. Графики рисует один `charts/ChartRenderer.kt` на `android.graphics.Canvas` и для экрана, и для PDF: меняя вид, проверять оба.
2. Схема БД: поднимать версию в `AppDatabase` и писать `Migration`; destructive fallback запрещён (сотрёт дневники). Бэкап (`data/Backup.kt`) должен знать о новых полях.
3. Проверка: `gradlew.bat :app:testDebugUnitTest :app:assembleRelease`, затем эмулятор `Medium_Phone_API_36.0` (SDK в `Z:\SDK`). Тестовые данные заливаются через debug-сборку (`run-as`) и Python sqlite; `sqlite3` на образе нет. PDF смотреть рендером страниц в PNG (pymupdf).
4. После изменения поведения обновлять README.md и README.ru.md.

## Версии и релизы (GitHub)

- Версия — одна строка в `VERSION` → `versionName`/`versionCode` (major*10000+minor*100+patch) и тег.
- `.github/workflows/build.yml`: PR в `main` — тесты и APK-артефакт; слияние в `main` публикует релиз `v<VERSION>`, если тега ещё нет (текст — `RELEASE_NOTES.md`, первая строка `# Mindfuck v<версия>`, проверка `scripts/check-release.ps1`).
- Подпись: секреты `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` (`scripts/make-keystore.ps1`), в Gradle приходят как `MINDFUCK_KEYSTORE_*`. Без них APK подписан отладочным ключом.
- Репозиторий `github.com/lemitz/Mindfuck`, ветка `main`, `gh` авторизован. `git.exe` — `C:\Program Files\Git\cmd`, не в PATH. `local.properties` не коммитится.
