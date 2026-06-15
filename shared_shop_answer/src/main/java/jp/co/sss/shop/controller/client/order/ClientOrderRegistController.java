package jp.co.sss.shop.controller.client.order;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.shop.bean.BasketBean;
import jp.co.sss.shop.bean.OrderItemBean;
import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.Item;
import jp.co.sss.shop.entity.Order;
import jp.co.sss.shop.entity.OrderItem;
import jp.co.sss.shop.entity.User;
import jp.co.sss.shop.form.OrderForm;
import jp.co.sss.shop.repository.ItemRepository;
import jp.co.sss.shop.repository.OrderItemRepository;
import jp.co.sss.shop.repository.OrderRepository;
import jp.co.sss.shop.repository.UserRepository;
import jp.co.sss.shop.service.BeanTools;
import jp.co.sss.shop.service.PriceCalc;
import jp.co.sss.shop.util.Constant;

/**
 * 注文管理 注文受付機能のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientOrderRegistController {

	/**
	 * 会員情報 リポジトリ
	 */
	@Autowired
	UserRepository userRpository;

	/**
	 * 商品情報 リポジトリ
	 */
	@Autowired
	ItemRepository itemRepository;

	/**
	 * 注文情報 リポジトリ
	 */
	@Autowired
	OrderRepository orderRepository;

	/**
	 * 注文商品情報 リポジトリ
	 */
	@Autowired
	OrderItemRepository orderItemRepository;

	/**
	 * セッション
	 */
	@Autowired
	HttpSession session;

	/**
	 * 合計金額計算サービス
	 */
	@Autowired
	PriceCalc priceCalc;
	/**
	 * Entity、Form、Bean間のデータ生成、コピーサービス
	 */
	@Autowired
	BeanTools beanTools;

	/**
	 * お届け先入力処理
	 *
	 * @return "redirect:client/order/address/input" 届け先入力画面 表示処理
	 */
	@RequestMapping(path = "/client/order/address/input", method = RequestMethod.POST)
	public String inputAddress() {

		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			//セッション情報がない場合、買い物かごからの遷移と判断し表示用の情報を生成
			// 注文情報入力フォームの初期化
			orderForm = new OrderForm();
			UserBean loginUserBean = (UserBean) session.getAttribute("user");
			User user = userRpository.findByIdAndDeleteFlag(loginUserBean.getId(), Constant.NOT_DELETED);
			BeanUtils.copyProperties(user, orderForm);
			orderForm.setPayMethod(Constant.DEFAULT_PAYMENT_METHOD);

			// 入力フォーム情報をセッションに保持
			session.setAttribute("orderForm", orderForm);
		}

		// 届け先入力画面 表示処理
		return "redirect:/client/order/address/input";
	}

	/**
	 * お届け先情報入力画面　表示処理
	 *
	 * @param model Viewとの値受渡し
	 * @return "client/order/address_input" お届け先入力画面
	 */
	@RequestMapping(path = "/client/order/address/input", method = RequestMethod.GET)
	public String inputAddress(Model model) {

		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/syserror";
		}
		model.addAttribute("orderForm", orderForm);

		BindingResult result = (BindingResult) session.getAttribute("result");
		if (result != null) {
			model.addAttribute("org.springframework.validation.BindingResult.orderForm", result);
			session.removeAttribute("result");
		}

		// 届け先入力画面　表示処理
		return "client/order/address_input";
	}

	/**
	 * 届け先入力フォーム　チェック処理
	 *
	 * @param form 入力フォーム
	 * @param result 入力チェック結果
	 * @return 入力値エラーあり："redirect:/client/order/address/input" 送付先入力画面 表示処理
	 *         入力値エラーなし："redirect:/client/order/payment/input" 支払方法入力画面　表示処理 
	 */
	@RequestMapping(path = "/client/order/payment/input", method = RequestMethod.POST)
	public String inputAddressCheck(@Valid @ModelAttribute OrderForm form, BindingResult result) {

		// セッションに届け先入力フォーム情報を保持
		session.setAttribute("orderForm", form);

		// 入力値エラーありの場合、送付先入力画面へ
		if (result.hasErrors()) {
			session.setAttribute("result", result);

			// 届け先入力画面　表示処理
			return "redirect:/client/order/address/input";
		}

		// 支払い方法入力画面　表示処理
		return "redirect:/client/order/payment/input";
	}

	/**
	 * 支払方法入力画面　表示
	 * @param model Viewとの値受渡し
	 * @return　"client/order/payment_input"　支払方法入力画面　表示
	 */
	@RequestMapping(path = "/client/order/payment/input", method = { RequestMethod.GET })
	public String inputPayment(Model model) {
		// セッションのオーダー情報入力フォームからお支払方法を取得
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/syserror";
		}
		Integer payMethod = orderForm.getPayMethod();
		if (payMethod == null) {
			// セッションに無い場合、デフォルトを設定
			payMethod = Constant.DEFAULT_PAYMENT_METHOD;
		}

		// リクエストスコープに支払方法を設定
		model.addAttribute("payMethod", payMethod);

		// お支払方法入力画面　表示
		return "client/order/payment_input";

	}

	/** 
	 * 支払方法入力画面　表示 確認画面で戻るボタン押下時の遷移
	 * @return　"client/order/payment_input"　支払方法入力画面　表示
	 */
	@RequestMapping(path = "/client/order/payment/back", method = RequestMethod.POST)
	public String inputPaymentBack() {
		// 届け先入力画面 表示処理
		return "redirect:/client/order/address/input";

	}

	/**
	 * 注文内容確認処理(お支払い方法取得)
	 *
	 * @param payMethod 支払方法　入力
	 * @return "redirect:client/order/check" 注文情報登録確認画面 表示
	 *
	 */
	@RequestMapping(path = "/client/order/check", method = RequestMethod.POST)
	public String orderCheck(Integer payMethod) {

		// お支払い方法を注文情報入力フォームに設定し、セッションに保持
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/syserror";
		}
		orderForm.setPayMethod(payMethod);
		session.setAttribute("orderForm", orderForm);

		// 注文確認画面　表示処理
		return "redirect:/client/order/check";
	}

	/**
	 * 注文内容在庫チェックと注文確認画面　表示処理
	 * @param model Viewとの値受渡し
	 * @return "client/order/check" 注文確認画面　表示
	 */
	@RequestMapping(path = "/client/order/check", method = RequestMethod.GET)
	public String orderCheck(Model model) {
		// セッションから注文入力情報を取得
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/syserror";
		}

		// 買い物かご商品リストをセッションから取得
		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeanList = ((List<BasketBean>) session.getAttribute("basketBeans"));
		if (basketBeanList == null || basketBeanList.size() == 0) {
			// 買い物かごが空の場合、エラー
			return "redirect:/syserror";
		}

		// 在庫調整買い物リスト
		List<BasketBean> newBasketBeanList = new ArrayList<BasketBean>();
		// 在庫不足の商品名リスト（在庫数1以上）
		List<String> itemNameListLessThan = new ArrayList<String>();
		// 在庫不足の商品名リスト（在庫数0）
		List<String> itemNameListZero = new ArrayList<String>();
		// 在庫不足の商品リスト（在庫数0）
		// List<BasketBean> itemListZero = new ArrayList<BasketBean>();

		List<OrderItemBean> orderItemBeanList = new ArrayList<OrderItemBean>();

		// 在庫数の確認と買い物リストの再構築

		for (BasketBean basketBean : basketBeanList) {
			Integer id = basketBean.getId();
			// 最新の商品情報をDBから取得
			Item item = itemRepository.findByIdAndDeleteFlag(id, Constant.NOT_DELETED);
			if (item == null) {
				// 商品情報が無い場合
				itemNameListZero.add(basketBean.getName());
				continue;
			}
			// 最新の商品情報から、買い物かご商品情報を生成
			BasketBean newBasketBean = new BasketBean(item.getId(), item.getName(), item.getStock());
			newBasketBean.setOrderNum(basketBean.getOrderNum());

			// 在庫数チェックと調整後の買い物かごリスト、注文商品情報リストの作成
			if (newBasketBean.getStock() == 0) {
				// 在庫数=0の場合、買い物かごリストおよび注文商品リストに追加せず、在庫数ゼロ商品名リスト追加
				itemNameListZero.add(basketBean.getName());

			} else if (newBasketBean.getOrderNum() > newBasketBean.getStock()) {
				// 注文数 > 在庫数の場合
				// 注文数を在庫数に一致させ、買い物かごリストに追加
				newBasketBean.setOrderNum(newBasketBean.getStock());
				newBasketBeanList.add(newBasketBean);

				// オーダー商品情報の作成とリスト追加
				orderItemBeanList.add(beanTools.generateOrderItemBean(item, newBasketBean));

				// 在庫数不足商品名リスト追加
				itemNameListLessThan.add(basketBean.getName());

			} else {
				//在庫に問題がない場合
				// 買い物かごリストへ注文商品追加
				newBasketBeanList.add(newBasketBean);

				// オーダー商品情報の作成とリスト追加
				orderItemBeanList.add(beanTools.generateOrderItemBean(item, newBasketBean));
			}
		}

		if (itemNameListLessThan.size() > 0) {
			// 在庫不足商品名をスコープ設定
			model.addAttribute("itemNameListLessThan", itemNameListLessThan);
		}
		if (itemNameListZero.size() > 0) {
			// 在庫ゼロ足商品名をスコープ設定
			model.addAttribute("itemNameListZero", itemNameListZero);
		}

		if (newBasketBeanList.size() > 0) {
			// セッションに調整後の買い物かごリスト保持
			session.setAttribute("basketBeans", newBasketBeanList);
			basketBeanList = newBasketBeanList;

		} else {
			// 買い物かご商品が無くなった場合
			// セッションから買い物かごリストを削除、
			session.removeAttribute("basketBeans");

		}

		// 注文情報をリクエストスコープに設定
		if (orderItemBeanList.size() > 0) {
			// 合計金額を算出
			int total = priceCalc.orderItemBeanPriceTotal(orderItemBeanList);

			// カード情報、注文商品情報をViewに渡す
			model.addAttribute("orderItemBeans", orderItemBeanList);
			model.addAttribute("total", total);
			model.addAttribute("orderForm", orderForm);
		}

		// 在庫不足の商品名をリクエストスコープに設定
		if (itemNameListLessThan.size() > 0) {
			model.addAttribute("itemNameListLessThan", itemNameListLessThan);
		}
		if (itemNameListZero.size() > 0) {
			model.addAttribute("itemNameListZero", itemNameListZero);
		}

		// 注文確認画面　表示
		return "client/order/check";
	}

	/**
	 * 注文登録完了の処理
	 *
	 * @return 在庫切れありの場合："redirect:/client/order/check" 注文確認画面　表示処理
	 *         在庫切れなしの場合："redirect:/client/order/complete" 注文完了画面　表示処理
	 */
	@RequestMapping(path = "/client/order/complete", method = RequestMethod.POST)
	public String orderComplete() {

		// 注文情報を取得
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");

		// 買い物かごの中身を取得
		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeanList = ((List<BasketBean>) session.getAttribute("basketBeans"));

		// 在庫数の確認
		// 最終段階で在庫調整が必要な場合、確認画面を再表示
		// ※注文確定時に在庫数が変化した場合を想定した処理（詳細設計書の「在庫数の扱い」の項を参照）
		Item errorItem = null;
		for (BasketBean basketBean : basketBeanList) {
			errorItem = itemRepository.findByIdAndStockLessThan(basketBean.getId(), basketBean.getOrderNum());
			if (errorItem != null) {
				// 在庫不足商品がある場合、注文確認画面を再表示
				return "redirect:/client/order/check";
			}
		}

		// 商品の在庫数から購入数を引いて、DB更新
		for (BasketBean basketBean : basketBeanList) {
			Item item = itemRepository.findById(basketBean.getId()).orElse(null);
			int stock = item.getStock() - basketBean.getOrderNum();
			item.setStock(stock);
			itemRepository.save(item);
		}

		// 注文テーブルに登録する（1件）
		Order order = new Order();

		// フォームに入力された情報を注文エンティティにコピー
		order.setPostalCode(orderForm.getPostalCode());
		order.setAddress(orderForm.getAddress());
		order.setName(orderForm.getName());
		order.setPhoneNumber(orderForm.getPhoneNumber());
		order.setPayMethod(orderForm.getPayMethod());
		Integer userId = ((UserBean) session.getAttribute("user")).getId();
		User user = userRpository.findById(userId).orElse(null);
		order.setUser(user);

		for (BasketBean basketBean : basketBeanList) {
			OrderItem orderItem = new OrderItem();
			orderItem.setQuantity(basketBean.getOrderNum());
			Item item = itemRepository.findById(basketBean.getId()).orElse(null);
			orderItem.setItem(item);
			orderItem.setOrder(order);
			// 購入時点の商品単価をsetしてDBへINSERTする
			orderItem.setPrice(item.getPrice());
			orderItemRepository.save(orderItem);
		}

		orderRepository.save(order);

		//11gだとエラー
		// 注文商品テーブルに登録する（商品の個数分）
		//		order = orderRepository.findTop1ByOrderByIdDesc();

		// セッションスコープの買い物かご情報を初期化
		session.removeAttribute("basketBeans");

		// セッションの注文入力情報を削除
		session.removeAttribute("orderForm");

		return "redirect:/client/order/complete";
	}

	/**
	 * 注文登録完了の処理
	 *
	 * @return 在庫切れありの場合："redirect:/client/basket/list/0" 買い物かご画面へ
	 *         在庫切れなしの場合："client/order/complete" 注文完了画面へ
	 */
	@RequestMapping(path = "/client/order/complete", method = RequestMethod.GET)
	public String orderCompleteFinish() {
		return "client/order/complete";
	}

}
