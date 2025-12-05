import xlrd
import json
from collections import defaultdict

# Read the ward file
wb = xlrd.open_workbook('danh-sach-3321-xa-phuong.xls')
ws = wb.sheet_by_index(0)

# Data structures
provinces = {}
districts = defaultdict(list)
wards_by_district = defaultdict(list)

# Map province codes to their English equivalents (used in our API)
province_code_map = {
    '01': 1,    # HN
    '79': 66,   # HCM
    # ... we'll build this dynamically
}

for i in range(1, ws.nrows):  # Skip header
    row = [ws.cell_value(i, j) for j in range(ws.ncols)]
    try:
        ward_code = str(int(float(row[0]))).zfill(5)
        ward_name = row[1].strip()
        cap = row[2].strip()
        province_code = str(int(float(row[4]))).zfill(2)
        province_name = row[5].strip()
        
        if province_code not in provinces:
            provinces[province_code] = province_name
    except:
        continue

print(f'Total provinces found: {len(provinces)}')
for code, name in sorted(provinces.items()):
    print(f'  {code}: {name}')

# Save province list
with open('provinces.json', 'w', encoding='utf-8') as f:
    json.dump(provinces, f, ensure_ascii=False, indent=2)

print('\nSaved to provinces.json')
