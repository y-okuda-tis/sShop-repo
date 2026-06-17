package jp.co.sss.shop.validator;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;

import jp.co.sss.shop.annotation.LoginCheck;
import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.User;
import jp.co.sss.shop.repository.UserRepository;
import jp.co.sss.shop.util.Constant;

/**
 * ログインチェックの独自検証クラス
 *
 * @author System Shared
 */
public class LoginValidator implements ConstraintValidator<LoginCheck, Object> {
	private String email;
	private String password;

	@Autowired
	UserRepository userRepository;

	@Autowired
	HttpSession session;

	/** ログイン失敗最大回数 */
	private static final int MAX_LOGIN_ATTEMPTS = 5;

	/** アカウントロック時間（分） */
	private static final int ACCOUNT_LOCK_MINUTES = 30;

	@Override
	public void initialize(LoginCheck annotation) {
		this.email = annotation.fieldEmail();
		this.password = annotation.fieldPassword();
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		BeanWrapper beanWrapper = new BeanWrapperImpl(value);
		boolean isValidFlg = false;
		String emailProp = (String) beanWrapper.getPropertyValue(this.email);
		String passwordProp = (String) beanWrapper.getPropertyValue(this.password);

		User user = userRepository.findByEmailAndDeleteFlag(emailProp, Constant.NOT_DELETED);

		if (user == null) {
			// ユーザ認証に失敗
			isValidFlg = false;
			addConstraintViolation(context, "{login.missing.message}");
		} else {
			// アカウントロック状態を確認
			if (isAccountLocked(user)) {
				// アカウントがロック中
				isValidFlg = false;
				addConstraintViolation(context, "{account.locked.message}");
			} else if (passwordProp.equals(user.getPassword())) {
				// パスワード一致：ログイン成功
				resetLoginFailureCount(user);
				createUserSession(user);
				isValidFlg = true;
			} else {
				// パスワード不一致：ログイン失敗
				incrementLoginFailureCount(user);
				isValidFlg = false;
				if (isAccountLocked(user)) {
					addConstraintViolation(context, "{account.locked.message}");
				} else {
					addConstraintViolation(context, "{login.missing.message}");
				}
			}
		}
		return isValidFlg;
	}

	/**
	 * アカウントがロック中かどうかを確認
	 * @param user ユーザエンティティ
	 * @return ロック中の場合true
	 */
	private boolean isAccountLocked(User user) {
		if (user.getAccountLockedUntil() == null) {
			return false;
		}
		LocalDateTime now = LocalDateTime.now();
		return now.isBefore(user.getAccountLockedUntil());
	}

	/**
	 * ログイン失敗回数をインクリメント
	 * @param user ユーザエンティティ
	 */
	private void incrementLoginFailureCount(User user) {
		int failureCount = (user.getLoginFailureCount() == null ? 0 : user.getLoginFailureCount()) + 1;
		user.setLoginFailureCount(failureCount);

		// 失敗回数が5回に達した場合、アカウントをロック
		if (failureCount >= MAX_LOGIN_ATTEMPTS) {
			LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES);
			user.setAccountLockedUntil(lockedUntil);
		}

		userRepository.save(user);
	}

	/**
	 * ログイン失敗回数をリセット
	 * @param user ユーザエンティティ
	 */
	private void resetLoginFailureCount(User user) {
		user.setLoginFailureCount(0);
		user.setAccountLockedUntil(null);
		userRepository.save(user);
	}

	/**
	 * ユーザセッション情報を作成
	 * @param user ユーザエンティティ
	 */
	private void createUserSession(User user) {
		UserBean userBean = new UserBean();
		userBean.setId(user.getId());
		userBean.setName(user.getName());
		userBean.setAuthority(user.getAuthority());

		// セッションスコープにログインしたユーザの情報を登録
		session.setAttribute("user", userBean);
	}

	/**
	 * 制約違反メッセージを追加
	 * @param context コンテキスト
	 * @param message メッセージキー
	 */
	private void addConstraintViolation(ConstraintValidatorContext context, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message)
				.addConstraintViolation();
	}
}
