create database amazon;
use amazon;

create table Customer (
usernameID int not null,
reviewsusername varchar(50) not null,
constraint pk_user Primary key (usernameID));


create table Review (
reviewID int not null,
reviewsrating int not null,
reviewstitle varchar(1000) not null,
reviewstext varchar(1000),
reviewsdoRecommend boolean,
reviewsnumHelpful int not null,
usernameID int not null,
constraint pk_review Primary key (reviewID),
constraint fk_user Foreign key (usernameID) references Customer(usernameID));

create table Brand (
brandID int not null,
brand varchar(10) not null,
constraint pk_brand Primary key (brandID));

create table Categories (
categoriesID int not null,
primaryCategories varchar(50) not null,
brandID int not null,
constraint pk_category Primary key (categoriesID),
constraint fk_category Foreign key (brandID) references Brand(brandID));

create table Product (
productID int not null,
productCode varchar(30) not null,
productName varchar(200) not null,
asins varchar(20),
categoriesID int not null,
constraint pk_product Primary key (productID),
constraint fk_product Foreign key (categoriesID) references Categories(categoriesID));

create table ProductDate (
productDateID int not null,
dateAdded Date not null,
constraint pk_productdt Primary key (productDateID));

create table ReviewDate (
reviewsDateID int not null,
reviewsdate Date not null,
constraint pk_reviewdt Primary key (reviewsDateID));

create table Associate (
reviewID int not null,
ProductID int not null,
ProductDateID int not null,
reviewsDateID int not null,
Primary key (reviewID,productID,productDateID,reviewsDateID),
constraint fk_review Foreign key (reviewID) references Review(reviewID),
constraint fk_productfk Foreign key (productID) references Product(productID),
constraint fk_productdt Foreign key (productDateID) references ProductDate(productDateID),
constraint fk_reviewdt Foreign key (reviewsDateID) references ReviewDate(reviewsDateID));
