/**
 * Shared master-data option lists for the employee module.
 *
 * Department / Designation stay free-text columns in the database (small
 * project, no normalization tables); these lists feed the dropdown
 * suggestions in forms and filters. Existing rows keep whatever value
 * they already have.
 */
export const DEPARTMENT_OPTIONS = [
  "Engineering",
  "Sales",
  "Production",
  "Fish Farming",
  "Mushroom Farming",
  "Finance",
  "HR",
  "Administration",
];

export const DESIGNATION_OPTIONS = [
  "Worker",
  "Manager",
  "Accountant",
  "Developer",
  "Supervisor",
  "Farm Assistant",
  "Sales Executive",
];
