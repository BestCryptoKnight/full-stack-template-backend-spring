# Fullstack boilerplate spring boot backend

This is a template spring boot REST API for fullstack app.  

It provides basic functionalities that are necessary for any app such as (detail description can be found here):

* **User registration**
    * username, email and password. Account has to be activated via email
    * O2 - Github, Google, Facebook
* **User login**
    * email + password
    * email + password + two-factor code. Two-factor can be set once user is logged in.
    * email + password + recovery code. In case two factor code can not be used.
    * O2 authentication - Github, Google, Facebook
    * O2 authentication + two-factor. Two-factor can be set once user is logged in
* **Forgotten password** - Password change request is send on email
* **Account management**
    * profile picture, email, username update. If email is updated the change has to be approved from new email -
      otherwise email will not be updated
    * password change
    * cancel account
* **i18n support**

Frontend template repositories: 
* [React](https://github.com/Hasatori/fullstack-boilerplate-react-frontend) - web support

## Configuration

Application can be configured via yml files that are stored at `src/main/resources`. There are tree configuration files:

* `application.yml` - common configuration for all environments
* `application-local.yml` - configuration for **_local_** environment
* `application-production.yml` - configuration for **_production_** environment

For security reasons sensitive information such as database username or O2 secrets are not exposed in the yml files but are set from system environment properties

| Property name           | Yaml paths            |                              
| -----------             | -----------           |
| DATASOURCE_URL          | spring.datasource.url                                           
| DATASOURCE_USERNAME     | spring.datasource.username                                      
| DATASOURCE_PASSWORD     | spring.datasource.password                                      
| GOOGLE_CLIENT_ID        | spring.security.oauth2.client.registration.google.clientId             
| GOOGLE_CLIENT_SECRET    | spring.security.oauth2.client.registration.google.clientSecret         
| FACEBOOK_CLIENT_ID      | spring.security.oauth2.client.registration.facebook.clientId             
| FACEBOOK_CLIENT_SECRET  | spring.security.oauth2.client.registration.facebook.clientSecret         
| GITHUB_CLIENT_ID        | spring.security.oauth2.client.registration.github.clientId             
| GITHUB_CLIENT_SECRET    | spring.security.oauth2.client.registration.github.clientSecret         
| MAIL_SERVER_HOST        | spring.mail.host; spring.mail.properties.mail.smtp.ssl.trust        
| MAIL_SERVER_USERNAME    | spring.mail.username         
| MAIL_SERVER_PASSWORD    | spring.mail.password
