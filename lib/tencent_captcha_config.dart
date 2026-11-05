class TencentCaptchaConfig {
  String? appId;
  // 自定义透传参数，业务可用该字段传递少量数据，该字段的内容会被带入callback回调的对象中
  dynamic bizState;
  // 开启自适应深夜模式 (true/false) 或强制深夜模式 ("force")
  dynamic enableDarkMode;
  // 示例 {"width": 140, "height": 140}
  // 移动端原生webview调用时传入，为设置的验证码弹框大小。
  Map<String, dynamic>? sdkOpts;
  // false: 隐藏帮助按钮，或自定义帮助链接: "url地址"
  dynamic needFeedBack;
  // 指定验证码提示文案的语言，优先级高于控制台配置
  String? userLanguage;
  // true: 开启适老化，false: 关闭适老化
  bool? enableAged;
  // 定义验证码展示方式，popup: （默认）弹出式
  String? type;
  // 是否在验证码加载过程中显示loading框，默认true
  bool? loading;
  // 设置CaptchaAppId 加密校验串
  String? aidEncrypted;
  // 设置可访问的验证码接入host，如海外接入域名ca.turing.captcha.qcloud.com及www.tycaptcha.com二选一
  String? captchaPageHost;
  // 设置验证码毫秒级超时时间，最小值为10s (10000)，默认60s (60000)
  int? captchaTimeoutMillis;
  // 开启后，对安全的请求可自动完成勾选验证，无需用户操作，默认为关闭
  bool? enableAutoCheck;
  // 支持灵活设置对应key及value值 (setExtractParameter)
  Map<String, dynamic>? extractParameters;
  // 验证码页面在 WebView 中的来源（https 网域）。为空时以 file:// 载入本地 html；
  // 腾讯 FAQ 指出 file:// / null 来源会被风控视为异常设备，建议设定 https baseUrl
  String? baseUrl;

  TencentCaptchaConfig({
    this.appId,
    this.bizState,
    this.enableDarkMode,
    this.sdkOpts,
    this.needFeedBack,
    this.userLanguage,
    this.enableAged,
    this.type,
    this.loading,
    this.aidEncrypted,
    this.captchaPageHost,
    this.captchaTimeoutMillis,
    this.enableAutoCheck,
    this.extractParameters,
    this.baseUrl,
  });

  Map<String, dynamic> toJson() {
    Map<String, dynamic> jsonObject = <String, dynamic>{};
    if (appId != null) jsonObject.putIfAbsent("appId", () => appId);
    if (bizState != null) jsonObject.putIfAbsent("bizState", () => bizState);
    if (enableDarkMode != null) {
      jsonObject.putIfAbsent("enableDarkMode", () => enableDarkMode);
    }
    if (sdkOpts != null) jsonObject.putIfAbsent("sdkOpts", () => sdkOpts);
    if (needFeedBack != null) {
      jsonObject.putIfAbsent("needFeedBack", () => needFeedBack);
    }
    if (userLanguage != null) {
      jsonObject.putIfAbsent("userLanguage", () => userLanguage);
    }
    if (enableAged != null) {
      jsonObject.putIfAbsent("enableAged", () => enableAged);
    }
    if (type != null) jsonObject.putIfAbsent("type", () => type);
    if (loading != null) jsonObject.putIfAbsent("loading", () => loading);
    if (aidEncrypted != null) {
      jsonObject.putIfAbsent("aidEncrypted", () => aidEncrypted);
    }
    if (captchaPageHost != null) {
      jsonObject.putIfAbsent("captchaPageHost", () => captchaPageHost);
    }
    if (captchaTimeoutMillis != null) {
      jsonObject.putIfAbsent(
          "captchaTimeoutMillis", () => captchaTimeoutMillis);
    }
    if (enableAutoCheck != null) {
      jsonObject.putIfAbsent("enableAutoCheck", () => enableAutoCheck);
    }
    if (extractParameters != null) {
      jsonObject.putIfAbsent("extractParameters", () => extractParameters);
    }
    if (baseUrl != null) jsonObject.putIfAbsent("baseUrl", () => baseUrl);

    return jsonObject;
  }
}
