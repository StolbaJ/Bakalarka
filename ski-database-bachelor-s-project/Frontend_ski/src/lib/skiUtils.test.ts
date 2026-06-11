import { dataToCreateSkiRequest, skiResponseToData } from '@/lib/skiUtils'

describe('skiUtils', () => {
  it('maps API ski response to UI data with Czech labels', () => {
    const result = skiResponseToData({
      id: 42,
      skiNumber: 'SKI-001',
      brand: 'Atomic',
      model: 'Redster',
      length: '170',
      year: 2023,
      skiType: null,
      weightKg: null,
      condition: 'DOBRY',
      status: 'V_SERVISU',
      location: 'Rack A',
      notes: null,
      lastServiceDate: '2025-01-10',
      nextServiceDate: null,
      struktura: null,
      strukturaRecordedAt: null,
      structureChangeCount: 2,
      ean: null,
      partNo: null,
      serialNo: null,
      skiUsage: null,
      isFromImportedOrder: false,
    })

    expect(result).toMatchObject({
      id: 'SKI-001',
      numericId: 42,
      condition: 'Dobrý',
      status: 'V servisu',
      location: 'Rack A',
      structureChangeCount: 2,
    })
  })

  it('maps UI form data to API request enums and year number', () => {
    const result = dataToCreateSkiRequest({
      brand: 'Fischer',
      model: 'RC4',
      length: '165',
      year: '2024',
      condition: 'Výborný',
      status: 'Dostupný',
      notes: 'Novy par',
      struktura: 'A1',
    })

    expect(result).toMatchObject({
      brand: 'Fischer',
      model: 'RC4',
      length: '165',
      year: 2024,
      condition: 'VYORNY',
      status: 'DOSTUPNY',
      notes: 'Novy par',
      struktura: 'A1',
    })
  })
})
