import xlrd
import pandas as pd

# Load the Excel file
file_path = r'd:\Website-b-n-thi-t-b-th-ng-minh-main\danh-sach-3321-xa-phuong.xls'

# First, try to get sheet names and basic info
try:
    wb = xlrd.open_workbook(file_path)
    sheet_names = wb.sheet_names()
    print('Sheet Names:')
    for i, sheet in enumerate(sheet_names, 1):
        print(f'  {i}. {sheet}')
except Exception as e:
    print(f'Error with xlrd: {e}')
    sheet_names = []

# Now use pandas to read and display data
try:
    print('\n--- First Sheet Analysis ---')
    # Read the first sheet
    df = pd.read_excel(file_path, sheet_name=0, engine='xlrd')
    sheet_name = sheet_names[0] if sheet_names else 'Unknown'
    print(f'Sheet name: {sheet_name}')
    print(f'Total rows (data): {len(df)}')
    print(f'Total columns: {len(df.columns)}')
    print(f'\nColumn Headers:')
    for col in df.columns:
        print(f'  - {col}')
    print(f'\nFirst 10 rows:')
    print(df.head(10).to_string())
except Exception as e:
    print(f'Error reading with pandas: {e}')
