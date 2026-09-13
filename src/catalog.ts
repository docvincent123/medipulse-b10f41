export type ProductChannel = 'Stable' | 'Beta';
export type ProductCategory = 'Clinical' | 'Education' | 'Diagnostics' | 'Platform';
export type LicenseType = 'free' | 'pro' | 'clinic' | 'preview';

export type Product = {
  id: string;
  name: string;
  shortName: string;
  description: string;
  category: ProductCategory;
  channel: ProductChannel;
  latestVersion: string;
  size: string;
  accent: string;
  icon: 'rehab' | 'calc' | 'case' | 'ecg' | 'glasses';
  licenseType: LicenseType;
  licenseLabel: string;
  priceLabel: string;
  releaseNotes: string[];
  featured?: boolean;
  status?: 'available' | 'coming-soon';
};

export const catalog: Product[] = [
  {
    id: 'rehaflow',
    name: 'RehaFlow',
    shortName: 'RF',
    description: 'Rehabilitation center operations, patient workflows, staff coordination and local-first clinical infrastructure.',
    category: 'Clinical',
    channel: 'Stable',
    latestVersion: '2.4.1',
    size: '186 MB',
    accent: '#31d6a6',
    icon: 'rehab',
    licenseType: 'clinic',
    licenseLabel: 'Clinic',
    priceLabel: 'Clinic license',
    featured: true,
    releaseNotes: ['Faster local sync', 'Improved discharge reports', 'Device enrollment and TLS diagnostics'],
  },
  {
    id: 'medical-calculator',
    name: 'Medical Calculator Pro',
    shortName: 'MC',
    description: 'Clinical scores, infusion math, dose tools and structured chart-ready results in one fast Windows workspace.',
    category: 'Clinical',
    channel: 'Beta',
    latestVersion: '0.8.0',
    size: '92 MB',
    accent: '#45b8ff',
    icon: 'calc',
    licenseType: 'pro',
    licenseLabel: 'Free + Pro',
    priceLabel: 'Free · Pro upgrade',
    releaseNotes: ['NEWS2 and GCS', 'Pro entitlement surface', 'Chart-text generator'],
  },
  {
    id: 'caselab',
    name: 'CaseLab',
    shortName: 'CL',
    description: 'Interactive emergency-care and cardiology cases for training, assessment and scenario rehearsal.',
    category: 'Education',
    channel: 'Beta',
    latestVersion: '0.5.3',
    size: '128 MB',
    accent: '#9b87ff',
    icon: 'case',
    licenseType: 'free',
    licenseLabel: 'Free',
    priceLabel: 'Free starter pack',
    releaseNotes: ['Scenario timeline', 'Assessment mode', 'Content-pack foundation'],
  },
  {
    id: 'ecg-studio',
    name: 'ECG Studio',
    shortName: 'ECG',
    description: 'ECG viewing, measurement, teaching overlays and case organization for study and clinical review.',
    category: 'Diagnostics',
    channel: 'Beta',
    latestVersion: '0.3.0',
    size: '104 MB',
    accent: '#ff7b91',
    icon: 'ecg',
    licenseType: 'preview',
    licenseLabel: 'Preview',
    priceLabel: 'Preview access',
    status: 'coming-soon',
    releaseNotes: ['Caliper tool', 'QT/QTc workspace', 'Case library'],
  },
  {
    id: 'vision-lab',
    name: 'QureMED Vision Lab',
    shortName: 'VL',
    description: 'Companion tools for accessibility glasses, sensors, camera workflows and experimental hardware.',
    category: 'Platform',
    channel: 'Beta',
    latestVersion: '0.1.2',
    size: '74 MB',
    accent: '#f4be53',
    icon: 'glasses',
    licenseType: 'preview',
    licenseLabel: 'Internal preview',
    priceLabel: 'Internal preview',
    status: 'coming-soon',
    releaseNotes: ['Device bridge shell', 'Sensor status cards', 'Voice-response workflow'],
  },
];
