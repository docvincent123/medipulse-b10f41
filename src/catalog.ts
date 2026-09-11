export type ProductChannel = 'Stable' | 'Beta';
export type ProductCategory = 'Clinical' | 'Education' | 'Diagnostics' | 'Platform';

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
  priceLabel: string;
  releaseNotes: string[];
  featured?: boolean;
};

export const catalog: Product[] = [
  {
    id: 'rehaflow',
    name: 'RehaFlow',
    shortName: 'RF',
    description: 'Rehabilitation center operations, patient workflows and staff coordination.',
    category: 'Clinical',
    channel: 'Stable',
    latestVersion: '2.4.1',
    size: '186 MB',
    accent: '#34d399',
    icon: 'rehab',
    priceLabel: 'Clinic license',
    featured: true,
    releaseNotes: ['Faster local sync', 'Improved discharge reports', 'Device enrollment fixes'],
  },
  {
    id: 'medical-calculator',
    name: 'Medical Calculator Pro',
    shortName: 'MC',
    description: 'Clinical scores, infusion math, dose tools and structured chart-ready results.',
    category: 'Clinical',
    channel: 'Beta',
    latestVersion: '0.8.0',
    size: '92 MB',
    accent: '#22d3ee',
    icon: 'calc',
    priceLabel: 'Free · Pro available',
    releaseNotes: ['Added NEWS2 and GCS', 'Pro demo entitlement', 'New chart-text generator'],
  },
  {
    id: 'caselab',
    name: 'CaseLab',
    shortName: 'CL',
    description: 'Interactive medical scenarios for emergency care, cardiology and training.',
    category: 'Education',
    channel: 'Beta',
    latestVersion: '0.5.3',
    size: '128 MB',
    accent: '#a78bfa',
    icon: 'case',
    priceLabel: 'Free starter pack',
    releaseNotes: ['Scenario timeline', 'Assessment mode', 'Pack marketplace foundation'],
  },
  {
    id: 'ecg-studio',
    name: 'ECG Studio',
    shortName: 'ECG',
    description: 'ECG viewing, measurement, teaching overlays and case organization.',
    category: 'Diagnostics',
    channel: 'Beta',
    latestVersion: '0.3.0',
    size: '104 MB',
    accent: '#fb7185',
    icon: 'ecg',
    priceLabel: 'Preview',
    releaseNotes: ['Caliper tool', 'QT/QTc workspace', 'Case library'],
  },
  {
    id: 'glasses-lab',
    name: 'QureMed Vision Lab',
    shortName: 'VL',
    description: 'Companion tools for accessibility glasses, sensors and experimental hardware.',
    category: 'Platform',
    channel: 'Beta',
    latestVersion: '0.1.2',
    size: '74 MB',
    accent: '#fbbf24',
    icon: 'glasses',
    priceLabel: 'Internal preview',
    releaseNotes: ['Device bridge shell', 'Sensor status cards', 'Voice response mock flow'],
  },
];
