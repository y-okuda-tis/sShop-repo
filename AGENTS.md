
# システム概要

このコードは Spring Bootで構成された プログラム教育用ECサイトです。
会員制のECサイトで、**一般ユーザー向け機能（購入者）**と**管理者向け機能**を備えています。

技術構成：

* Spring Boot 4
* Spring MVC
* Spring Data JPA
* Thymeleaf
* Oracle Database（ojdbc11）
* Bean Validation
* Maven
* Java 17

---

# 主な機能

## 1. ユーザー機能

### 会員管理

* 新規会員登録
* ログイン／ログアウト
* 会員情報変更
* 退会

関連Controller

* ClientUserRegistController
* ClientUserShowController
* ClientUserUpdateController
* ClientUserDeleteController
* LoginController
* LogoutController

---

### 商品閲覧

* 商品一覧表示
* 商品詳細表示
* カテゴリ別検索
* 人気商品表示

関連Controller

* ClientItemShowController

---

### カート機能

* 商品をカートへ追加
* カート内商品一覧表示
* 数量変更
* 削除

関連Controller

* ClientBasketController

---

### 注文機能

* 配送先入力
* 注文確認
* 注文確定
* 注文履歴表示
* 注文詳細表示

関連Controller

* ClientOrderRegistController
* ClientOrderShowController

---

# 2. 管理者機能

管理画面（Admin Menu）から各マスタを管理できます。

---

## 商品管理

* 商品一覧
* 商品登録
* 商品編集
* 商品削除
* 商品画像アップロード

関連Controller

* AdminItemShowController
* AdminItemRegistController
* AdminItemUpdateController
* AdminItemDeleteController

---

## カテゴリ管理

* カテゴリ一覧
* カテゴリ登録
* カテゴリ編集
* カテゴリ削除

関連Controller

* AdminCategoryShowController
* AdminCategoryRegistController
* AdminCategoryUpdateController
* AdminCategoryDeleteController

---

## 会員管理

* 会員一覧
* 会員登録
* 会員編集
* 会員削除

関連Controller

* AdminUserShowController
* AdminUserRegistController
* AdminUserUpdateController
* AdminUserDeleteController

---

## 注文管理

* 注文一覧
* 注文詳細確認

関連Controller

* AdminOrderShowController

---

# データベース構造

主要テーブルは5つです。

| テーブル        | 内容     |
| ----------- | ------ |
| users       | 会員情報   |
| categories  | 商品カテゴリ |
| items       | 商品情報   |
| orders      | 注文情報   |
| order_items | 注文明細   |

---

# エンティティ関係

```text
User
 └─ Order
      └─ OrderItem
           └─ Item
                └─ Category
```

### 関係

* 1人の会員 → 複数注文
* 1注文 → 複数商品
* 1カテゴリ → 複数商品

---

# 特徴的な実装

## 人気商品ランキング

`ItemRepository`

```java
SELECT ...
GROUP BY ...
ORDER BY COUNT(i.id) DESC
```

購入回数の多い商品を人気順で表示しています。

---

## 画像アップロード

`UploadFileService`

* 商品画像アップロード対応
* ファイル名に日時を付与
* 重複防止

例：

```text
20260617103015_apple.jpg
```

---

## 料金計算

`PriceCalc`

* 小計計算
* 合計金額計算
* 注文時価格計算

を担当しています。

---

# 画面構成

## 一般ユーザー

```text
トップページ
 ├─ ログイン
 ├─ 会員登録
 ├─ 商品一覧
 │    └─ 商品詳細
 ├─ カート
 └─ 注文
      ├─ 配送先入力
      ├─ 確認
      └─ 完了
```

## 管理者

```text
管理メニュー
 ├─ 商品管理
 ├─ カテゴリ管理
 ├─ 会員管理
 └─ 注文管理
```

---
# 開発時の禁止事項

* pom.xmlの変更を生じる新たなライブラリの導入は禁止（CDNはOK）
* サービスレイヤは含まず、基礎的なＭＶＣで構成する（Controllerから直接JPAリポジトリのメソッドを呼んでいる）
* コーディング規約はクラスはUpperキャメル、メソッドはlowerキャメルなど原則、Google Java Styleに準拠する
