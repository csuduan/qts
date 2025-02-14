from setuptools import setup, find_packages
import os

setup(
    name="qts",
    version="0.1.0",
    packages=find_packages(),
    install_requires=[
        # 在这里列出项目的依赖包
    ],
    author="Your Name",
    author_email="your.email@example.com",
    description="common utilities for qts",
    long_description=open("README.md").read() if os.path.exists("README.md") else "",
    long_description_content_type="text/markdown",
    url="https://your-repo-url.com",
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.6",
) 