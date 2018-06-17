1-2-Authenticate
================
Copyright 2018 Wilco van Beijnum

Description
-----------
1-2-Authenticate is an app for two factor authentication using open standards developed by
the [Initiative for Open Authentication (OATH)](http://www.openauthentication.org/).

This implementation supports the HMAC-Based One-time Password (HOTP) algorithm
specified in [RFC 4226](https://tools.ietf.org/html/rfc4226) and the Time-based
One-time Password (TOTP) algorithm specified in [RFC 6238](https://tools.ietf.org/html/rfc6238).

This app is a modified version of the open source version of the Google Authenicator app (Copyright 2010 Google Inc.), which can be found [here](https://github.com/google/google-authenticator-android).

Features
--------
- Add secrets by scanning a QR-code or adding the key manually
- Customize the look of an entry to distinguish it from the other entries
  - Useful if you have many entries
  - Add a custom icon
  - Set a custom color
- Export and import keys to and from a file
  - Save a backup of your secrets on an external medium
  - Easily transfer secrets to a other/new device without having to trust a cloud service
  - Can be exported as plain JSON or as a file encrypted with AES encryption
- And of course fully open source