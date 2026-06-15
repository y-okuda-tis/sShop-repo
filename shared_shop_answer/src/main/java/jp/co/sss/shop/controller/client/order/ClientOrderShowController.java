package jp.co.sss.shop.controller.client.order;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.shop.bean.OrderBean;
import jp.co.sss.shop.bean.OrderItemBean;
import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.Order;
import jp.co.sss.shop.entity.OrderItem;
import jp.co.sss.shop.repository.OrderRepository;
import jp.co.sss.shop.service.BeanTools;
import jp.co.sss.shop.service.PriceCalc;

/**
 * 注文管理 一覧表示機能(一般会員用)のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientOrderShowController {

	/**
	 * 注文情報
	 */
	@Autowired
	OrderRepository orderRepository;

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
	 * 注文情報一覧表示処理
	 *
	 * @param model    Viewとの値受渡し
	 * @return "client/order/list" 一覧画面　表示
	 */
	@RequestMapping(path = "/client/order/list", method = { RequestMethod.GET, RequestMethod.POST })
	public String showOrderList(Model model) {

		// セッションスコープより会員情報を取得
		UserBean userBean = ((UserBean) session.getAttribute("user"));
		if (userBean == null) {
			// 対象が無い場合、エラー
			return "redirect:/syserror";
		}

		// 会員IDに該当する情報のみを取得
		List<Order> orderList = orderRepository.findByUserIdOrderByInsertDateDescIdDesc(userBean.getId());

		// 注文情報リストを生成
		List<OrderBean> orderBeanList = new ArrayList<OrderBean>();
		for (Order order : orderList) {
			OrderBean orderBean = new OrderBean();
			orderBean.setId(order.getId());
			orderBean.setUserName(order.getUser().getName());
			orderBean.setInsertDate(order.getInsertDate().toString());
			orderBean.setPayMethod(order.getPayMethod());

			// orderレコードから紐づくOrderItemのListを取り出す
			List<OrderItem> orderItemList = order.getOrderItemsList();
			//合計金額の算出
			int total =priceCalc.orderItemPriceTotal(orderItemList);

			//合計金額のセット
			orderBean.setTotal(total);
			
			orderBeanList.add(orderBean);
		}

		// 注文情報リストをViewへ渡す
		model.addAttribute("orders", orderBeanList);

		return "client/order/list";
	}

	/**
	 * 詳細表示 処理
	 *
	 * @param id   詳細表示対象ID
	 * @param model   Viewとの値受渡し
	 * @return "client/order/detail" 注文情報 詳細画面へ
	 */
	@RequestMapping(path = "/client/order/detail/{id}")
	public String showOrder(@PathVariable int id, Model model) {

		// 選択された注文情報に該当する情報を取得
		Order order = orderRepository.findById(id).orElse(null);

		// 表示する注文情報を生成
		OrderBean orderBean = new OrderBean();
		BeanUtils.copyProperties(order, orderBean);
		orderBean.setInsertDate(order.getInsertDate().toString());

		
		// 注文商品情報を取得
		List<OrderItemBean> orderItemBeanList=beanTools.generateOrderItemBeanList(order.getOrderItemsList());

		// 合計金額を算出
		int total = priceCalc.orderItemBeanPriceTotalUseSubtotal(orderItemBeanList);

		// 注文情報をViewへ渡す
		model.addAttribute("order", orderBean);
		model.addAttribute("orderItemBeans", orderItemBeanList);
		model.addAttribute("total", total);

		return "client/order/detail";

	}

}
