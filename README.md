# Full stack template - Spring boot 


<p align="center">
 <img src="./docs/logo.png" width="400">
</p>

Full stack template is a React/Spring based template webapp that provides set of functionalities that are necessary for almost any web app. The code and all components were structured so that project is easy to test, maintain and expand.

This is a repository for the backend part written in Spring boot.

Frontend can be found [HERE](https://github.com/Hasatori/fullstack-boilerplate-react-frontend)

Api documentation can be found at:  <a href="https://fullstack-template-spring.herokuapp.com/swagger-ui.html" target="_blank">fullstack-template-spring.herokuapp.com/swagger-ui.html</a>

![swagger_front_page](./docs/swagger_front_page.png)

## Installation

1) Package app `mvn clean install`
2) Start app `java -jar -Dspring.profiles.active=<active_profiles> <jar_name> `


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

## General supported functionalities
### User registration
  * username, email and password. Account has to be activated via email
  * O2 - Github, Google, Facebook
### User login
  * email + password
  * email + password + two-factor code. Two-factor can be set once user is logged in.
  * email + password + recovery code. In case two factor code can not be used.
  * O2 authentication - Github, Google, Facebook
  * O2 authentication + two-factor. Two-factor can be set once user is logged in
### Forgotten password 
   * Password change request is send on email
### Account management
  * profile picture, email, username update. If email is updated the change has to be approved from new email -
    otherwise email will not be updated
  * password change
  * cancel account

