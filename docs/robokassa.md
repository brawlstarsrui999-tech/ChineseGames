# Robokassa, магазин украшений и реклама

> **Правило ChineseGames:** все уроки, HSK-курсы, словарь и игровые режимы остаются полностью бесплатными. Через Robokassa продаются **только** косметические и необязательные улучшения. Перед оплатой приложение показывает уведомление: **средства не возвращаются**.

## Товары и цены

| Код (`Shp_product`) | Товар | Цена |
|---|---|---:|
| `custom_music` | Свой фоновый трек с устройства | 99 ₽ |
| `color_themes` | Все цветные стили (фиолетовый всегда бесплатен) | 199 ₽ |
| `style_china` | Китайский дракон, декор и традиционная музыка | 199 ₽ |
| `style_clover` | Розовый клевер: падающие листья, декор и воздушная музыка | 199 ₽ |
| `mascot` | Кловерушка: зверушка-талисман с мурр-трелями | 499 ₽ |
| `no_ads` | Навсегда убрать баннер | 99 ₽ |

Покупка хранится локально в `chinese_games_purchases` и объединяется с Firestore после Google Sign-In. Не выдавайте товар только по return URL: единственный источник истины — подтверждённый статус Robokassa.


## Тестовые промокоды

В итоговой витрине есть компактное поле **«Промокод»** — оно нужно только автору и тестовой команде и не содержит технических пояснений. Каждый код применяется один раз на устройстве, сразу открывает соответствующий товар и затем синхронизируется через Google-аккаунт как обычная покупка.

| Промокод | Открывает |
|---|---|
| `RUIN4IK-PROMO20091` | Китайский стиль |
| `RUIN4IK-PROMO20092` | Розовый клевер |
| `RUIN4IK-PROMO20093` | Цветные стили |
| `RUIN4IK-PROMO20094` | Кловерушку-талисман |
| `RUIN4IK-PROMO20095` | Свою музыку |
| `RUIN4IK-PROMO20096` | «Без рекламы» |

## 1. Настройка Robokassa

1. Создайте магазин в [личном кабинете Robokassa](https://partner.robokassa.ru/).
2. В **Технических настройках** запишите `MerchantLogin`, **Пароль #1** и **Пароль #2**.
3. Для отладки включите тестовый режим. В тестовом режиме реальные деньги не списываются.
4. Укажите в кабинете адреса возврата:
   - Success URL: `https://chinesegames.app/pay/success`
   - Fail URL: `https://chinesegames.app/pay/fail`

   Это маркеры для WebView: после любого возврата Android всё равно запрашивает `OpStateExt`, поэтому фальшивый переход на Success URL ничего не разблокирует.
5. Для production задайте **Result URL** на своём HTTPS-сервере. Он обязан перепроверить подпись `OutSum:InvId:Password#2` и сохранить оплату у себя. Клиентская проверка статуса — только запасной UX-механизм, а не замена серверному webhook.

Подробный формат XML статуса — в [документации Robokassa: XML-интерфейсы](https://docs.robokassa.ru/ru/xml-interfaces). Успешный платёж имеет `State.Code = 100`; `50` означает, что деньги получены, но зачисление ещё продолжается.

## 2. Секреты — только вне Git

Создайте/дополните **локальный** `local.properties` (он уже в `.gitignore`):

```properties
# Robokassa: для локального теста можно держать тестовые данные.
ROBOKASSA_LOGIN=YourMerchantLogin
ROBOKASSA_PASSWORD1=your-test-password-1
ROBOKASSA_PASSWORD2=your-test-password-2
ROBOKASSA_TEST=true

# Для production предпочтительна серверная подпись. Тогда Password #1
# в APK можно оставить пустым:
ROBOKASSA_SIGN_URL=https://api.example.com/robokassa/create-payment

# Необязательно: реальные AdMob ID. Без них используются официальные test ID Google.
ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
ADMOB_BANNER_ID=ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
```

Либо передайте те же ключи как Gradle properties: `./gradlew assembleRelease -PROBOKASSA_LOGIN=...`.

`app/build.gradle.kts` генерирует эти значения в `BuildConfig`. **Не храните Password #1 в production APK.** Укажите `ROBOKASSA_SIGN_URL`: backend принимает `{ product, invId, outSum, description, email, isTest }`, сам делает MD5 и возвращает `{ "url": "https://auth.robokassa.ru/..." }`.

### Локальная подпись (только тест/простая схема)

Ссылка имеет параметры:

```text
https://auth.robokassa.ru/Merchant/Index.aspx
  ?MerchantLogin=<login>
  &OutSum=199.00
  &InvId=<invoice>
  &Description=ChineseGames%3A...
  &Shp_product=style_clover
  &SignatureValue=md5(<login>:199.00:<invoice>:<password1>:Shp_product=style_clover)
  &IsTest=1
```

`Shp_*` параметры добавляются в строку подписи **после Password #1**, в алфавитном порядке. В приложении эта логика находится в `billing/RobokassaClient.kt`.

### Проверка счёта

После WebView приложение делает запрос:

```text
GET https://auth.robokassa.ru/Merchant/WebService/Service.asmx/OpStateExt
  ?MerchantLogin=<login>
  &InvoiceID=<invoice>
  &Signature=md5(<login>:<invoice>:<password2>)
  &IsTest=1
```

Товар открывается при коде `100` (также интерфейс терпимо ожидает переходное `50`). Отменённые (`10`) и возвращённые (`60`) счета закрываются, остальные остаются доступными для кнопки «Восстановить покупки».

## 3. Firebase Google Sign-In и Firestore

1. Создайте Android-приложение в Firebase с package name **`com.chinesegames.app`**.
2. Добавьте SHA-1/SHA-256 сертификатов debug и release в Firebase / Google Cloud Console.
3. Скачайте настоящий `google-services.json` в `app/google-services.json`. Он игнорируется Git; в репозитории есть только `app/google-services.json.example`.
4. В Authentication включите provider **Google**; создайте/скопируйте **Web client ID** и добавьте в `local.properties`:

   ```properties
   CG_WEB_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com
   ```

5. Создайте Firestore в production mode. Минимальные rules для владельца документа:

   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

Без настоящего `google-services.json` Gradle-плагин Google Services намеренно не применяется, а экран настроек выводит «Firebase не настроен» вместо падения приложения.

Firestore документ `users/{uid}` содержит `purchases`, `settings`, `hskGroups`, `hskExams`, `hskSentenceTopics`. При синхронизации покупки объединяются множеством; `passedMask` групп объединяется OR, лучшие результаты и попытки берутся максимумом — прогресс никогда не откатывается.

## 4. AdMob

В проекте по умолчанию стоят официальные тестовые ID Google:

- App ID: `ca-app-pub-3940256099942544~3347511713`
- Banner ID: `ca-app-pub-3940256099942544/6300978111`

До публикации замените их своими `ADMOB_APP_ID` и `ADMOB_BANNER_ID` в `local.properties`. Не кликайте собственные реальные баннеры. Баннер показывается только на спокойных экранах; покупка `no_ads` скрывает его навсегда.
