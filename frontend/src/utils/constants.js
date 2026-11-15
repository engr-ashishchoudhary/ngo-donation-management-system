// Values mirror the backend enums exactly.
export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  DONOR: 'DONOR',
};

export const ADMIN_ROLES = [ROLES.SUPER_ADMIN, ROLES.ADMIN];

export const CAMPAIGN_STATUSES = ['ACTIVE', 'CLOSED', 'COMPLETED'];
export const DONATION_STATUSES = ['PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'];
export const PAYMENT_METHODS = ['CASH', 'BANK_TRANSFER', 'UPI', 'CARD', 'CHEQUE'];
export const EXPENSE_CATEGORIES = [
  'OPERATIONS',
  'LOGISTICS',
  'MARKETING',
  'SALARIES',
  'SUPPLIES',
  'OTHER',
];

// Allowed donation transitions (BR-13): PENDING -> COMPLETED|FAILED, COMPLETED -> REFUNDED
export const DONATION_TRANSITIONS = {
  PENDING: ['COMPLETED', 'FAILED'],
  COMPLETED: ['REFUNDED'],
  FAILED: [],
  REFUNDED: [],
};

export const STORAGE_KEYS = {
  TOKEN: 'ngo.auth.token',
  USER: 'ngo.auth.user',
};
