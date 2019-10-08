import random

__all__ = ['SUBJECT_ID', 'SUBJECT_NAMESPACE', 'CODES', 'OPEN_EHR']

def gen_valid_subject_id():
    ''' generates random strings to be used as subjectId and subjectNamespace
    '''
    random_int = random.randint(0, 10000)
    random_str1 = ['a','b','c','d','e','x','y','z'][random.randint(0,7)]
    random_str2 = random.choice(['a','b','c','d','e','x','y','z'])
    return random_str1 + random_str2 + str(random_int)

SUBJECT_ID = gen_valid_subject_id()
SUBJECT_NAMESPACE = "snamespace_" + SUBJECT_ID
OPEN_EHR = "OpenEHR is the Future!"

CODES = [200, 404, 500]
