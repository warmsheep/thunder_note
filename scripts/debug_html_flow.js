const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright-core');

async function run() {
  const htmlPath = path.resolve(__dirname, '..', 'thunder-note-prototype.html');
  const targetUrl = `file://${htmlPath}`;
  const outputDir = path.resolve(__dirname, '..', 'artifacts', 'html-debug');
  fs.mkdirSync(outputDir, { recursive: true });

  const browser = await chromium.launch({
    headless: true,
    executablePath: '/bin/chromium-browser',
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
  });
  const context = await browser.newContext({ viewport: { width: 430, height: 932 } });
  const page = await context.newPage();

  let stepNo = 0;
  const log = [];
  const record = (message) => {
    stepNo += 1;
    const line = `${String(stepNo).padStart(2, '0')}. ${message}`;
    log.push(line);
    console.log(line);
  };
  const shot = async (name) => {
    const file = path.join(outputDir, `${String(stepNo).padStart(2, '0')}-${name}.png`);
    await page.screenshot({ path: file, fullPage: true });
    console.log(`    screenshot: ${file}`);
  };

  page.on('dialog', async (dialog) => {
    console.log(`    dialog: ${dialog.type()} => ${dialog.message()}`);
    if (dialog.type() === 'confirm') {
      await dialog.dismiss();
    } else {
      await dialog.accept();
    }
  });

  try {
    record('打开原型页面');
    await page.goto(targetUrl);
    await page.waitForTimeout(1800); // 等待启动页自动跳到登录页
    await shot('login');

    record('打开服务器设置，切换到自定义服务器并保存');
    await page.locator('.server-link').click();
    await page.locator('#server-settings .server-option-item').nth(1).click();
    await page.locator('#custom-server-url').fill('https://demo.example.com');
    await page.locator('#save-server-btn').click();
    await page.locator('.back-arrow').click();
    await shot('server-settings-saved');

    record('进入注册页并返回登录页');
    await page.locator('#login .auth-link').click();
    await page.waitForSelector('#register.active');
    await shot('register');
    await page.locator('#register .auth-link').click();
    await page.waitForSelector('#login.active');

    record('填写登录信息并登录');
    await page.locator('#login-email').fill('tester@example.com');
    await page.locator('#login-password').fill('123456');
    await page.locator('#login button:has-text("登录")').click();
    await page.waitForSelector('#notes-page.active');
    await shot('notes-home');

    record('打开新建闪记弹窗，选择头像并创建新闪记');
    await page.locator('#notes-page .add-btn').click();
    await page.locator('#new-note-name').fill('自动化测试闪记');
    await page.locator('#avatar-grid .avatar-option').nth(3).click();
    await page.locator('#create-modal button:has-text("创建")').click();
    await shot('note-created');

    record('打开第一条闪记进入聊天页');
    await page.locator('#notes-list .note-item').first().click();
    await page.waitForSelector('#chat-page.active');
    await shot('chat-opened');

    record('发送文本消息');
    await page.locator('#chat-input').fill('这是一条自动化发送的文本消息');
    await page.locator('.send-btn').click();

    const imagePath = path.join(outputDir, 'temp-upload.png');
    fs.writeFileSync(
      imagePath,
      Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIW2NkYGD4DwABBAEAgh2j4QAAAABJRU5ErkJggg==',
        'base64',
      ),
    );

    record('展开工具栏，发送图片+文案');
    await page.locator('.add-btn-chat').click();
    await page.locator('.tool-item:has-text("图片")').click();
    await page.setInputFiles('#image-file-input', imagePath);
    await page.waitForSelector('.image-preview-item');
    await page.locator('#image-caption-input').fill('图片备注：自动化测试');
    await page.locator('#image-send-btn').click();

    record('再次展开工具栏，发送视频和文件');
    await page.locator('.add-btn-chat').click();
    await page.locator('.tool-item:has-text("视频")').click();
    await page.locator('.add-btn-chat').click();
    await page.locator('.tool-item:has-text("文件")').click();
    await shot('chat-rich-messages');

    record('长按语音按钮并发送语音');
    await page.locator('#voice-btn').dispatchEvent('mousedown');
    await page.waitForTimeout(1300);
    await page.locator('#voice-btn').dispatchEvent('mouseup');
    await page.locator('.voice-action:has-text("发送")').click();
    await shot('chat-voice');

    record('点击聊天更多按钮');
    await page.locator('.chat-more').click();

    record('返回闪记首页并切换到合集页');
    await page.locator('.back-btn').click();
    await page.waitForSelector('#notes-page.active');
    await page.locator('.tab-item:has-text("合集")').click();
    await page.waitForSelector('#collections-page.active');
    await shot('collections');

    record('切换到收藏页');
    await page.locator('.tab-item:has-text("收藏")').click();
    await page.waitForSelector('#favorites-page.active');
    await shot('favorites');

    record('切换到我的页');
    await page.locator('.tab-item:has-text("我的")').click();
    await page.waitForSelector('#profile-page.active');
    await shot('profile');

    record('在我的页修改头像');
    await page.locator('.menu-item:has-text("修改头像")').click();
    await page.locator('#avatar-select-grid .avatar-option').nth(5).click();
    await page.locator('#avatar-modal button:has-text("确定")').click();

    record('在我的页修改昵称');
    await page.locator('.menu-item:has-text("修改昵称")').click();
    await page.locator('#nickname-input').fill('自动化用户');
    await page.locator('#nickname-modal button:has-text("确定")').click();

    record('在我的页修改密码（先触发不一致校验，再正确提交）');
    await page.locator('.menu-item:has-text("修改密码")').click();
    await page.locator('#new-password').fill('abcdef');
    await page.locator('#confirm-password').fill('abcdeg');
    await page.locator('#password-modal button:has-text("确定")').click();
    await page.locator('#confirm-password').fill('abcdef');
    await page.locator('#password-modal button:has-text("确定")').click();
    await shot('profile-edits');

    record('打开设置页，切换暗黑模式，查看关于与致谢');
    await page.locator('.menu-item:has-text("设置")').click();
    await page.locator('.settings-item:has-text("暗黑模式") .toggle-slider').click();
    await page.locator('.settings-item:has-text("关于")').click();
    await page.locator('#about-modal button:has-text("确定")').click();
    await page.locator('.settings-item:has-text("致谢")').click();
    await page.locator('#thanks-modal button:has-text("确定")').click();
    await shot('settings');

    record('在设置页点击官网、隐私政策、反馈BUG、退出登录（取消确认）');
    await page.locator('.settings-item:has-text("官网")').click();
    await page.locator('.settings-item:has-text("隐私政策")').click();
    await page.locator('.settings-item:has-text("反馈BUG")').click();
    await page.locator('.settings-item:has-text("退出登录")').click();
    await page.evaluate(() => closeSettingsModal());
    await shot('settings-close');

    record('从我的页执行退出登录');
    await page.locator('#profile-page .menu-item:has-text("退出登录")').click();
    await page.waitForSelector('#login.active');
    await shot('logout-to-login');

    fs.writeFileSync(path.join(outputDir, 'steps.log'), `${log.join('\n')}\n`, 'utf8');
    console.log(`\n步骤日志已写入: ${path.join(outputDir, 'steps.log')}`);
  } finally {
    await context.close();
    await browser.close();
  }
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
