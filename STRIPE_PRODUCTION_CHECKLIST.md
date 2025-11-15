# Stripe Production Deployment Checklist

**Application:** Beauty Center
**Date:** November 13, 2025

---

## Pre-Production Setup

### 1. ✅ Obtain Live Stripe Keys

- [ ] Login to https://dashboard.stripe.com
- [ ] Switch to "Live mode" (top-right toggle)
- [ ] Go to Developers → API keys
- [ ] Copy your **Publishable key** (`pk_live_...`)
- [ ] Copy your **Secret key** (`sk_live_...`)

---

### 2. ✅ Configure Environment Variables

#### Frontend (Vercel)
Go to Vercel Dashboard → Project Settings → Environment Variables

```
REACT_APP_STRIPE_PUBLISHABLE_KEY=pk_live_51...
```

#### Backend (Production Server)
Set environment variables:

```bash
# For Linux/Mac
export STRIPE_SECRET_KEY=sk_live_51...
export STRIPE_PUBLISHABLE_KEY=pk_live_51...

# For systemd service (recommended)
# Edit /etc/systemd/system/beauty-center.service
[Service]
Environment="STRIPE_SECRET_KEY=sk_live_51..."
Environment="STRIPE_PUBLISHABLE_KEY=pk_live_51..."
```

Or use `.env` file (ensure it's in `.gitignore`):
```
STRIPE_SECRET_KEY=sk_live_51...
STRIPE_PUBLISHABLE_KEY=pk_live_51...
```

---

### 3. ✅ Stripe Account Configuration

#### Enable Fraud Protection
- [ ] Go to Stripe Dashboard → Radar
- [ ] Enable **Stripe Radar for Fraud Teams** (recommended)
- [ ] Configure rules:
  - Block payments over €1000 without review
  - Flag high-risk countries (optional)
  - Enable 3D Secure when risky

#### Configure Email Receipts
- [ ] Go to Settings → Email settings
- [ ] Enable "Successful payments" emails
- [ ] Customize receipt template with your branding
- [ ] Test email with a test payment

#### Set Up Webhooks (Optional but Recommended)
- [ ] Go to Developers → Webhooks
- [ ] Click "+ Add endpoint"
- [ ] Enter your endpoint URL: `https://your-backend.com/api/webhook/stripe`
- [ ] Select events to listen for:
  - `payment_intent.succeeded`
  - `payment_intent.payment_failed`
  - `charge.refunded`
  - `charge.dispute.created`
- [ ] Copy the **Signing secret** (`whsec_...`)
- [ ] Add to backend environment:
  ```
  STRIPE_WEBHOOK_SECRET=whsec_...
  STRIPE_WEBHOOK_ENABLED=true
  ```

---

### 4. ✅ Business Information

- [ ] Complete business profile in Stripe Dashboard
- [ ] Add company logo
- [ ] Set business address
- [ ] Add support email and phone
- [ ] Configure statement descriptor (appears on customer's bank statement)
  - Recommended: "SUCCAR BANAT" or "BEAUTE SUCCAR"

---

### 5. ✅ Banking & Payout Setup

- [ ] Go to Settings → Bank accounts and scheduling
- [ ] Add bank account for payouts
- [ ] Verify bank account (micro-deposits or instant verification)
- [ ] Set payout schedule:
  - Recommended: Daily automatic payouts
  - Or: Manual payouts for more control

---

## Testing in Staging

### 6. ✅ Staging Environment Test

**Use live keys in staging first!**

#### Test with Stripe Test Cards (in test mode)
- [ ] Card: `4242 4242 4242 4242` (Visa)
- [ ] Expiry: Any future date
- [ ] CVC: Any 3 digits
- [ ] Verify payment succeeds

#### Test Scenarios
- [ ] **Small payment** (€10)
- [ ] **Large payment** (€500)
- [ ] **Failed payment** (use card `4000 0000 0000 0002`)
- [ ] **Authentication required** (use card `4000 0027 6000 3184`)
- [ ] **Balance top-up**
- [ ] **Order checkout**

#### Verify After Each Test
- [ ] Payment appears in Stripe Dashboard
- [ ] Balance updated correctly in database
- [ ] Transaction recorded in BalanceTransaction
- [ ] User receives email receipt (if enabled)
- [ ] No errors in application logs

---

## Production Deployment

### 7. ✅ Deploy to Production

#### Frontend Deployment
```bash
cd beauty-center-frontend
vercel --prod
```

Verify environment variables are set in Vercel:
- [ ] `REACT_APP_STRIPE_PUBLISHABLE_KEY=pk_live_...`

#### Backend Deployment
```bash
cd /home/bahae/SuccarBanat
mvn clean package -DskipTests
# Deploy JAR to production server
```

Verify environment variables:
- [ ] `STRIPE_SECRET_KEY=sk_live_...`

---

### 8. ✅ Production Smoke Test

**Use a REAL card with a small amount (€1-5)**

- [ ] Create new account or use test account
- [ ] Add €5 to balance
- [ ] Verify charge appears in Stripe Dashboard
- [ ] Verify balance updated in application
- [ ] Check bank statement (will show pending charge)
- [ ] Refund the test payment in Stripe Dashboard

---

### 9. ✅ Monitoring Setup

#### Stripe Dashboard Alerts
- [ ] Go to Settings → Team & security → Notifications
- [ ] Enable alerts for:
  - Failed payments (>5% failure rate)
  - Disputes created
  - Large payments (>€500)
  - Radar blocks

#### Application Monitoring
- [ ] Set up log monitoring for:
  - `Failed to create payment intent`
  - `Failed to retrieve payment intent`
  - `PaymentIntent has already been applied`
  - `This PaymentIntent doesn't belong to user`

#### Stripe Sigma (Optional - for analytics)
- [ ] Query payment trends
- [ ] Monitor refund rates
- [ ] Track payment methods

---

### 10. ✅ Security Verification

- [ ] No test keys in production code
- [ ] `.env` files in `.gitignore`
- [ ] HTTPS enforced on all endpoints
- [ ] CORS configured to production domain only
- [ ] Security headers enabled (from OWASP ZAP fixes)
- [ ] Rate limiting active

---

## Post-Deployment

### 11. ✅ Customer Communication

- [ ] Update Terms of Service with:
  - Payment processor: Stripe
  - Refund policy
  - Billing descriptor on statements

- [ ] Update Privacy Policy with:
  - Payment data processing by Stripe
  - Link to Stripe Privacy Policy

- [ ] Add payment info to FAQ:
  - Accepted payment methods
  - When will I be charged?
  - Refund timeline
  - Billing descriptor

---

### 12. ✅ Support Preparation

#### Common Issues & Solutions

**"Payment declined"**
- Customer should contact their bank
- May be fraud prevention
- Try different card

**"3D Secure authentication failed"**
- Customer needs to complete verification with bank
- Retry payment

**"Duplicate payment"**
- Check transaction history
- If duplicate charge, refund in Stripe Dashboard

**"Refund request"**
- Process refunds in Stripe Dashboard
- Refunds take 5-10 business days

#### Stripe Support
- [ ] Save Stripe support email: support@stripe.com
- [ ] Bookmark Stripe Dashboard: https://dashboard.stripe.com
- [ ] Save fraud helpline: Available in dashboard

---

## Ongoing Maintenance

### Daily
- [ ] Check Stripe Dashboard for:
  - Failed payments
  - Disputes
  - Unusual activity

### Weekly
- [ ] Review payment success rate (should be >95%)
- [ ] Check for new disputes
- [ ] Review Radar blocks (false positives?)

### Monthly
- [ ] Review total payment volume
- [ ] Check refund rate (<5% is healthy)
- [ ] Update payment limits if needed
- [ ] Review and respond to disputes

### Quarterly
- [ ] Review Stripe fees
- [ ] Optimize payment flow based on data
- [ ] Update to latest Stripe SDK versions
- [ ] Re-run security audit

---

## Emergency Procedures

### If You Suspect Fraud
1. Go to Stripe Dashboard → Radar
2. Block the suspicious customer
3. Refund any fraudulent charges
4. Contact Stripe support

### If Payments Are Failing
1. Check Stripe Status: https://status.stripe.com
2. Check your server logs
3. Verify API keys are correct
4. Test with a new payment
5. Contact Stripe support if needed

### If You Need to Disable Payments
1. Remove Stripe keys from environment variables
2. Deploy update
3. Show maintenance message to users

---

## Success Metrics

After 30 days, verify:
- [ ] Payment success rate: >95%
- [ ] No security incidents
- [ ] Dispute rate: <1%
- [ ] Average payment processing time: <3 seconds
- [ ] Customer satisfaction with payment flow

---

## Rollback Plan

If critical issues occur:

1. **Immediate:**
   - Switch back to test keys (disables live payments)
   - Deploy previous version

2. **Investigate:**
   - Check logs
   - Check Stripe Dashboard
   - Identify root cause

3. **Fix and Redeploy:**
   - Fix issue in staging
   - Test thoroughly
   - Deploy to production

---

## Sign-Off

### Pre-Production
- [ ] Live Stripe keys obtained
- [ ] Environment variables configured
- [ ] Staging tests passed
- [ ] Security verification complete

**Signed:** _________________ **Date:** _________________

### Production Go-Live
- [ ] Production deployment successful
- [ ] Smoke tests passed
- [ ] Monitoring configured
- [ ] Team notified

**Signed:** _________________ **Date:** _________________

---

## Support Contacts

**Stripe Support:**
- Dashboard: https://dashboard.stripe.com
- Email: support@stripe.com
- Phone: Available in dashboard (varies by country)
- Documentation: https://stripe.com/docs

**Internal Team:**
- Developer: ________________
- Operations: ________________
- Finance: ________________

---

**Checklist Version:** 1.0
**Last Updated:** November 13, 2025
