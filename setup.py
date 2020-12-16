# -*- coding: utf-8 -*-
from setuptools import find_packages, setup
with open("README.md", "r") as fh:
    long_description = fh.read()
# python setup.py install
setup(
    name='oknem',
    version='1.0.1',
    packages=find_packages(include=['oknem']),
    include_package_data=True,
    zip_safe=False,
    install_requires=[
        'flask',
    ],

    author="jamesfchen",
    author_email="hawksjamesf@gmail.com",
    description="oknem",
    long_description=long_description,
    long_description_content_type="text/markdown",
    license="Apache2",
    url='https://jamesfchen.github.io/pyadb/',
    # keywords="",
    # url='',
    classifiers=[
        # How mature is this project? Common values are
        #   3 - Alpha
        #   4 - Beta
        #   5 - Production/Stable
        'Development Status :: 5 - Production/Stable',

        # Pick your license as you wish (should match "license" above)
        'License :: OSI Approved :: Apache Software License',

        # Specify the Python versions you support here. In particular, ensure
        # that you indicate whether you support Python 2, Python 3 or both.
        # 'Programming Language :: Python :: 2.7',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.3',
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
        'Programming Language :: Python :: 3.6',
    ],
    entry_points={
        'console_scripts': [
            'oknem=oknem:create',
        ],
    }
)
