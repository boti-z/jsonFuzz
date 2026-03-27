#!/usr/bin/python3

import copy
import json
import time
import re

class combitoCrema():

	def __init__(self):
		self.permutations = []

	def unicode_encode(self,texto):
		res = ''.join(r'\u{:04X}'.format(ord(chr)) for chr in texto) #Aux function para encodear un string en Unicode solo
		return res

	def strings_permutations(self, jsonData):
		#v1 Unicode things
		"""strings=[value for value in jsonData.values() if isinstance(value, str)]
		for string_val1 in strings:
			last_char=string_val1[-1]
			unicode_encoded_char=self.unicode_encode(last_char)
			temp_obj=copy.deepcopy(jsonData)
			for key, value in temp_obj.items():
				if value == string_val1:
					temp_obj[key]= string_val1[:-1]+unicode_encoded_char #replace char
					self.permutations.append(temp_obj)
					break"""
		####################################################################################
		#Empieza lo del unicode. Añade \ud888 al final y encodea el ultimo char		
		strings = [value for value in jsonData.values() if isinstance(value, str)]

		for text in strings:
		    # Primera permutación: Agregar el valor Unicode \ud888 al final de la cadena
		    temp_obj = copy.deepcopy(jsonData)
		    for key, value in temp_obj.items():
		        if value == text:
		            temp_obj[key] += r'\ud888'
		            break
		    self.permutations.append(temp_obj)

		    # Segunda permutación: Codificar en Unicode el último carácter de la cadena
		    temp_obj = copy.deepcopy(jsonData)
		    for key, value in temp_obj.items():
		        if value == text:
		            last_char = value[-1]
		            temp_obj[key] = value[:-1] + self.unicode_encode(last_char)
		            break
		    self.permutations.append(temp_obj)

		####################################################################################
		#Mete un byte nulo en unicode
		strings = [value for value in jsonData.values() if isinstance(value, str)]

		for text in strings:
		    # Primera permutación: Agregar el valor Unicode \ud888 al final de la cadena
		    temp_obj = copy.deepcopy(jsonData)
		    for key, value in temp_obj.items():
		        if value == text:
		            temp_obj[key] += r'\u0000ef'
		            break
		    self.permutations.append(temp_obj)

		####################################################################################
		##Modulo para meter strings dentro de JSON objects y Arrays
		strings = [value for value in jsonData.values() if isinstance(value, str)]

		for string_val in strings:
		    # Permutación con el parámetro metido en un Array
		    array_permutation = copy.deepcopy(jsonData)
		    for key, value in array_permutation.items():
		        if value == string_val:
		            array_permutation[key] = [string_val]
		            self.permutations.append(array_permutation)

		    # Permutación metiendo el valor en un JSON Object
		    json_object_permutation = copy.deepcopy(jsonData)
		    for key, value in json_object_permutation.items():
		        if value == string_val:
		            json_object_permutation[key] = {key: string_val}
		            self.permutations.append(json_object_permutation)

		    # Permutación metiendo el valor en un array, añadiendo otro string al array
		    array_with_extra_string_permutation = copy.deepcopy(jsonData)
		    for key, value in array_with_extra_string_permutation.items():
		        if value == string_val:
		            array_with_extra_string_permutation[key] = [string_val, "wsg127"]
		            self.permutations.append(array_with_extra_string_permutation)

		    # Permutación metiendo el array en un JSON object junto a otro string
		    json_object_with_array_permutation = copy.deepcopy(jsonData)
		    for key, value in json_object_with_array_permutation.items():
		        if value == string_val:
		            json_object_with_array_permutation[key] = {key: [string_val, "wsg127"]}
		            self.permutations.append(json_object_with_array_permutation)

	def numbers_permutations(self, jsonData):
		#Sustituir por 3.14...
		numbers=[value for value in jsonData.values() if isinstance(value, int) or isinstance(value, float)]
		for i, num in enumerate(numbers):
			temp_obj=copy.deepcopy(jsonData)
			for key,value in temp_obj.items():
				if value == num:
					temp_obj[key]= 3.1415926
					break
					self.permutations.append(temp_obj)

		#Permutacion con exponencial
		numbers=[value for value in jsonData.values() if isinstance(value, int) or isinstance(value, float)]
		for i, num in enumerate(numbers):
			temp_obj=copy.deepcopy(jsonData)
			for key,value in temp_obj.items():
				if value == num:
					temp_obj[key]= 6e5
					break
			self.permutations.append(temp_obj)

		#Permutacion con exponencial negativo
		numbers=[value for value in jsonData.values() if isinstance(value, int) or isinstance(value, float)]
		for i, num in enumerate(numbers):
			temp_obj=copy.deepcopy(jsonData)
			for key,value in temp_obj.items():
				if value == num:
					temp_obj[key]= 1.602e-19
					break
			self.permutations.append(temp_obj)

		#Permutacion con exponencial fraccion
		numbers=[value for value in jsonData.values() if isinstance(value, int) or isinstance(value, float)]
		for i, num in enumerate(numbers):
			temp_obj=copy.deepcopy(jsonData)
			for key,value in temp_obj.items():
				if value == num:
					temp_obj[key]= 123.456e7
					break
			self.permutations.append(temp_obj)

		#Permutacion con int boundary
		numbers=[value for value in jsonData.values() if isinstance(value, int) or isinstance(value, float)]
		for i, num in enumerate(numbers):
			temp_obj=copy.deepcopy(jsonData)
			for key,value in temp_obj.items():
				if value == num:
					temp_obj[key]= 9007199254740993
					break
			self.permutations.append(temp_obj)

		#Permutacion con int boundary negativa
		numbers=[value for value in jsonData.values() if isinstance(value, int) or isinstance(value, float)]
		for i, num in enumerate(numbers):
			temp_obj=copy.deepcopy(jsonData)
			for key,value in temp_obj.items():
				if value == num:
					temp_obj[key]= -9007199254740993
					break
			self.permutations.append(temp_obj)

		#Duplicar params
		number=[value for value in jsonData.values() if isinstance(value, int) or isinstance(value, float)]
		for num in numbers:
			temp_obj= copy.deepcopy(jsonData)
			for key,value in temp_obj.items():
				if value == num:
					temp_obj[key + "_double"]= value
					self.permutations.append(temp_obj.copy())
					break

	def generatePermutation(self,jsonData):
		def permute(obj):
			if isinstance(obj, dict):
				for key, value in list(obj.items()):
					if isinstance(value, list):
						# Duplicar elementos del array
						temp_obj = copy.deepcopy(obj)
						temp_obj[key] = value * 2
						self.permutations.append(temp_obj.copy())
					elif isinstance(value, dict):
						# Duplicar el objeto entero
						temp_obj = copy.deepcopy(obj)
						temp_obj[key]=value
						temp_obj[key+"_duplicate"] = copy.deepcopy(value)
						self.permutations.append(temp_obj.copy())
						permute(value)  # Explorar recursivamente el objeto duplicado
		
		permute(jsonData)
		time.sleep(2)
		self.strings_permutations(jsonData)  #llama a strings
		self.numbers_permutations(jsonData)  #llama a numbers permutations

		return self.permutations
		

def main():
	"""json_input ='''{
	    "string": "value",
	    "number": 2,
	    "literal":null,
	    "array": ["string", 2],
	    "object": {
	        "foo": "val"
	    }
	}'''"""
	json_input ='''{"protocol":"json","version":1}'''

	"""json_inputDup = {
    "string": "value",
    "number": 2,
    "literal": None,
    "array": ["string", 2],
    "object": {
        "foo": "val",
        "foo_duplicate": {
            "inner_key": "inner_value"
        	}
   		}
	}"""

	json_input = json.loads(json_input)
	test=combitoCrema()

	output = test.generatePermutation(json_input)

	for idx, permutation in enumerate(output, start=1):
		print(f"{permutation}")

main()
