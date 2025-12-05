import json

with open('data/ghn-master.json', encoding='utf-8') as f:
    data = json.load(f)

print("\n=== HANOI DISTRICTS (ProvinceID=1) ===")
for d in data['districts']:
    if d.get('ProvinceID') == 1:
        print(f"{d.get('DistrictID')} - {d.get('DistrictName')}")

print("\n=== HCM DISTRICTS (ProvinceID=202) ===")
for d in data['districts']:
    if d.get('ProvinceID') == 202:
        print(f"{d.get('DistrictID')} - {d.get('DistrictName')}")

