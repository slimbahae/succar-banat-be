Fixed runners tags

# SuccarBanat (Beauty Center)

A full-stack beauty center management application with a React frontend and a Spring Boot backend.

---

## Project Structure

- **Frontend:** React (with Tailwind CSS, Stripe integration, Firebase, etc.)
- **Backend:** Spring Boot (with MongoDB, JWT authentication, Stripe payments, email, etc.)

---

## Prerequisites

- **Node.js** (v16+ recommended)
- **npm** (v8+ recommended)
- **Java** (11 or 17 recommended)
- **MongoDB** (local instance)
- (Optional) **Stripe** account for payments
- (Optional) **Gmail** account for email sending

---

## 1. Running the Backend

### a. Clone the repository

```bash
git clone https://github.com/slimbahae/SuccarBanat.git
cd SuccarBanat
```

### b. Configure Environment

1. Rename `src/main/resources/application.properties.example` to `application.properties` (if you use a template).
2. Fill in your configuration values:
   - MongoDB connection (default: `localhost:27017`, database: `beauty-center-dev`)
   - Stripe keys (`stripe.secret`, `stripe.publishable`, etc.)
   - Email credentials (use [App Passwords](https://support.google.com/accounts/answer/185833) for Gmail, not your main password!)
   - JWT secret (generate a secure random string in production)

**Caution:** Never commit real secrets or passwords to version control.

### c. Start MongoDB

Make sure your MongoDB instance is running locally:

```bash
mongod
```

### d. Run the Spring Boot App

With Maven:

```bash
./mvnw spring-boot:run
```

Or with Gradle:

```bash
./gradlew bootRun
```

The backend should start on `http://localhost:8083` (as per `server.port`).

---

## 2. Running the Frontend

### a. Navigate to the frontend directory

If your React app is in a subfolder (e.g., `beauty-center-frontend`):

```bash
cd beauty-center-frontend
```

### b. Install dependencies

```bash
npm install
```

### c. Start the development server

```bash
npm start
```

The frontend should start on `http://localhost:3000` and proxy API requests to the backend (`http://localhost:8083`).

---

## 3. Configuration for Development

- **CORS:** The backend allows requests from `localhost:3000` (React dev server).
- **Stripe:** Use your Stripe test keys and webhook secret for development.
- **Email:** Enter your Gmail credentials (App Password recommended).
- **Environment Variables:** Never commit real secrets. Use `.env` files or keep secrets in `application.properties` locally.

---

## 4. Useful Scripts

**Frontend:**

- `npm start` — Run React dev server
- `npm run build` — Build for production
- `npm test` — Run tests

**Backend:**

- Standard Spring Boot lifecycle (`run`, `test`, etc.)

---

## 5. Additional Notes

- If you change API ports, update the `proxy` in `package.json` and CORS settings in `application.properties`.
- For production, use secure, non-default secrets and environment variables.
- For Stripe webhooks, use a tool like [`stripe-cli`](https://stripe.com/docs/stripe-cli) to forward events to your local backend.

---

## 6. Example `.env` for Frontend (if used)

```env
REACT_APP_STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key
REACT_APP_FIREBASE_API_KEY=your_firebase_api_key
# Add other frontend environment variables as needed
```

---

## 7. Example `application.properties` for Backend

```
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=beauty-center-dev
server.port=8083

jwt.secret=your_secret
jwt.expiration=3600000

stripe.secret=your_stripe_secret
stripe.publishable=your_stripe_publishable_key
stripe.webhook.secret=your_webhook_secret

spring.mail.username=your@email.com
spring.mail.password=your_app_password
```

---

## 8. Contact

For questions or contributions, open an issue or contact the repository owner.

---

Happy coding!
