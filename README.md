## MagicAuth

MagicAuth is an authentication app designed to alleviate the frustrations of OTP authentication when being logged out.

Normally, when a user is logging in and needs an OTP code, they fumble around for their phone or other device, open up the app, then have to enter it on their computer. 
This process takes times, is clumsy, and a strong barrier of entry for OTP adoption.

MagicAuth, instead, is designed to take away part of that pain - by using a browser extension to communicate between the users' computer and phone, the user can request a one time code, then authorise that request on their phone using biometrics or a PIN. 
This code is then sent back to the browser, and automatically entered.
This process would ideally be automatically detected when an OTP field is required, and a user could navigate to the necessary page in app after clicking on a notification.

In addition, MagicAuth is designed to try and be as open as possible; while other apps may support cloud backups, they tend to only work through a closed proprietary service, while MagicAuth is structured to use a FOSS server that can be replaced with a self-hosted one. 
Alternatively, you can back up your secrets to an encrypted file for storage on a hard drive.

MagicAuth is still in early development, but so far progress is good. Here are a few screenshots of progress.

![Account Drawer](demo/Account%20Drawer.png)

![QR Popup](demo/QR%20Popup.png)

![Slide to Delete](demo/Slide%20to%20Delete.png)

![Speed Dial](demo/Speed%20Dial.png)