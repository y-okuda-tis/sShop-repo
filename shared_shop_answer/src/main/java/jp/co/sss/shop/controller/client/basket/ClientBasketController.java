package jp.co.sss.shop.controller.client.basket;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.shop.bean.BasketBean;
import jp.co.sss.shop.entity.Item;
import jp.co.sss.shop.repository.ItemRepository;
import jp.co.sss.shop.util.Constant;

/**
 * 買い物かご機能のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientBasketController {

	/**
	 * 商品情報
	 */
	@Autowired
	ItemRepository itemRepository;

	/**
	 * セッション
	 */
	@Autowired
	HttpSession session;

	/**
	 * 商品を買い物かごに追加する処理
	 *
	 * @param id 追加対象商品ID
	 * @return ログインしている "redirect:/client/basket/shopping_basket" 買い物かご画面注文一覧1ページ目 
	 *  		ログインしていない "redirect:/login" ログイン画面
	 */
	@RequestMapping(path = "/client/basket/add", method = RequestMethod.POST)
	public String addItem(Integer id) {

		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeanList = ((List<BasketBean>) session.getAttribute("basketBeans"));

		// 買い物かごが空だった場合、新しくリストを作成
		if (basketBeanList == null) {
			basketBeanList = new ArrayList<BasketBean>();
		}

		// すでに買い物かごに入れられている商品か否かを調べる
		boolean itemExistsInBasket = false;
		for (BasketBean basketBean : basketBeanList) {
			if (basketBean.getId() == id) {
				// 注文ID(item.getId())が買い物かご内の商品IDと一致していた場合、処理を飛ばすフラグを立てる
				itemExistsInBasket = true;
				// 注文個数を1増加する(在庫チェックは、買い物かご一覧表示処理にて実施)
				basketBean.setOrderNum(basketBean.getOrderNum() + 1);
				break;
			}
		}

		// 処理を飛ばすフラグが立っていなかったら、商品情報を買い物かごに追加する
		if (!itemExistsInBasket) {
			// 追加対象商品情報を取得
			Item item = itemRepository.findByIdAndDeleteFlag(id, Constant.NOT_DELETED);
			if (item == null) {
				// 所品情報が無い場合、エラー
				return "redirect:/syserror";
			}
			BasketBean basketBean = new BasketBean(item.getId(), item.getName(), item.getStock());
			// 買い物かごの商品を追加する（商品は新しく追加した順に表示するため、先頭に追加する）
			// 在庫チェックは、一覧表示処理にて実施
			basketBeanList.add(0, basketBean);
			session.setAttribute("basketBeans", basketBeanList);
		}

		// 買い物かご　一覧表示処理
		return "redirect:/client/basket/list";
	}

	/**
	 * 買い物かごの商品個数を1減らす。
	 *
	 * @param id 削除対象所品ID
	 * @return "forward:/client/basket/list" 注文一覧1ページ目
	 */
	@RequestMapping(path = "/client/basket/delete", method = RequestMethod.POST)
	public String subtractCountItem(Integer id) {

		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeanList = ((List<BasketBean>) session.getAttribute("basketBeans"));
		if (basketBeanList == null) {
			basketBeanList = new ArrayList<BasketBean>();
		}

		for (BasketBean basketBean : basketBeanList) {
			// 買い物かごリストより対象商品を探す
			if (basketBean.getId() == id) {
				basketBean.setOrderNum(basketBean.getOrderNum() - 1);
				break;
			}
		}

		// 買い物かごリストをセッションスコープに保持
		session.setAttribute("basketBeans", basketBeanList);

		// 買い物かご内の商品一覧を表示
		return "redirect:/client/basket/list";

	}

	/**
	 * 買い物かごから商品をすべて削除する処理
	 *
	 * @return "redirect:/client/basket/list" 買い物かご画面 表示処理
	 */
	@RequestMapping(path = "/client/basket/allDelete", method = RequestMethod.POST)
	public String deleteAll() {

		// セッションスコープから買い物かごリストを削除
		session.removeAttribute("basketBeans");

		return "redirect:/client/basket/list";

	}

	/**
	 * 買い物かご内の商品一覧を表示する処理、表示時点の在庫数と比較し表示メッセージを作る
	 *
	 * @param model Viewとの値受渡し
	 * @return "client/basket/list" 買い物かご画面表示
	 */
	@RequestMapping(path = "/client/basket/list", method = { RequestMethod.POST, RequestMethod.GET })
	public String basketList(Model model) {

		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeanList = ((List<BasketBean>) session.getAttribute("basketBeans"));
		if (basketBeanList == null) {
			basketBeanList = new ArrayList<BasketBean>();
		}

		List<BasketBean> newBasketBeanList = new ArrayList<BasketBean>();
		List<String> itemNameListLessThan = new ArrayList<String>();
		List<String> itemNameListZero = new ArrayList<String>();

		// 在庫数と注文数のチェック
		for (BasketBean basketBean : basketBeanList) {
			if (basketBean.getOrderNum() <= 0) {
				// 注文がない(全削除)場合、買い物かごリストへ追加しない
				continue;
			}
			// 最新の商品情報をDBから取得
			Item item = itemRepository.findByIdAndDeleteFlag(basketBean.getId(), Constant.NOT_DELETED);
			if (item == null) {
				// 商品情報が無い場合
				itemNameListZero.add(basketBean.getName());
				continue;
			}

			// 最新の商品情報から、買い物かご商品情報を生成
			BasketBean newBasketBean = new BasketBean(item.getId(), item.getName(), item.getStock());
			newBasketBean.setOrderNum(basketBean.getOrderNum());

			if (newBasketBean.getStock() == 0) {
				// 在庫数=0の場合、買い物かごリストに追加せず、在庫数ゼロ商品名リスト追加
				itemNameListZero.add(basketBean.getName());
				continue;
			}
			if (newBasketBean.getOrderNum() > newBasketBean.getStock()) {
				// 注文数 > 在庫数の場合、注文数を在庫数に一致させ、買い物かごリストに追加
				newBasketBean.setOrderNum(newBasketBean.getStock());
				newBasketBeanList.add(newBasketBean);
				// 在庫数不足商品名リスト追加
				itemNameListLessThan.add(newBasketBean.getName());
				continue;
			}
			// 買い物かごリストへ注文商品追加
			newBasketBeanList.add(newBasketBean);
		}

		if (newBasketBeanList.size() > 0) {
			// セッションに買い物かごリスト保持
			session.setAttribute("basketBeans", newBasketBeanList);
		} else {
			session.removeAttribute("basketBeans");
		}

		// リクエストスコープに在庫調整メッセージの設定
		if (itemNameListLessThan.size() > 0) {
			// 在庫不足商品名をスコープ設定
			model.addAttribute("itemNameListLessThan", itemNameListLessThan);
		}
		if (itemNameListZero.size() > 0) {
			// 在庫ゼロ商品名をスコープ設定
			model.addAttribute("itemNameListZero", itemNameListZero);
		}

		//セッションから注文登録入力情報削除
		session.removeAttribute("orderForm");

		// 買い物かご画面　表示
		return "client/basket/list";
	}
}
